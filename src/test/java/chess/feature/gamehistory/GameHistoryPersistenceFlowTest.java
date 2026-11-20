package chess.feature.gamehistory;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.feature.game.event.GameFinishedEvent;
import chess.feature.game.model.PersistState;
import chess.feature.game.model.TimeControl;
import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.event.GameHistoryListener;
import chess.feature.gamehistory.mapper.GameHistoryMapper;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import chess.feature.gamehistory.service.GameHistoryPersister;
import chess.feature.gamehistory.service.GameHistoryService;
import chess.feature.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration-style test for the GameFinishedEvent -> listener -> mapper ->
 * persister -> executor -> GameHistoryService -> repository pipeline.
 * <p>
 * The repository is mocked deliberately: this test verifies the application
 * pipeline and the document handed to MongoRepository, not MongoDB itself.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GameHistoryPersistenceFlowTest.TestConfig.class)
@DisplayName("Game history persistence flow")
class GameHistoryPersistenceFlowTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GameHistoryRepository repository;

    @Autowired
    private GameHistoryPersister persister;

    @BeforeEach
    void setUp() {
        reset(repository);
        doAnswer(invocation -> invocation.getArgument(0))
                .when(repository).save(any(GameHistory.class));
        assertEquals(0, persister.pendingCount(), "A previous test left retry work pending");
    }

    @Test
    @DisplayName("Should map and save history asynchronously when the game-finished event is published")
    void shouldPersistMappedHistoryAsynchronously() throws Exception {
        GameSession session = createFinishedSession();
        CountDownLatch saveStarted = new CountDownLatch(1);
        CountDownLatch allowSaveToFinish = new CountDownLatch(1);

        doAnswer(invocation -> {
            saveStarted.countDown();
            assertTrue(
                    allowSaveToFinish.await(ASYNC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                    "Test did not release the repository save operation in time"
            );
            return invocation.getArgument(0);
        }).when(repository).save(any(GameHistory.class));

        try {
            // Publish from another thread so the test can prove that event handling
            // returns while the executor is still blocked in repository.save().
            CompletableFuture<Void> publish = CompletableFuture.runAsync(() -> {
                eventPublisher.publishEvent(new GameFinishedEvent(session));
                // A duplicate event must not submit a second persistence operation.
                eventPublisher.publishEvent(new GameFinishedEvent(session));
            });

            assertTrue(saveStarted.await(ASYNC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            publish.get(1, TimeUnit.SECONDS);
            assertEquals(PersistState.PENDING, session.getPersistState());
        } finally {
            allowSaveToFinish.countDown();
        }

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertTrue(session.isSaved()));

        var captor = org.mockito.ArgumentCaptor.forClass(GameHistory.class);
        verify(repository, times(1)).save(captor.capture());

        GameHistory saved = captor.getValue();
        assertEquals(session.getGameId(), saved.getGameId());
        assertEquals("white-player", saved.getWhitePlayerId());
        assertEquals("black-player", saved.getBlackPlayerId());
        assertEquals(List.of("white-player", "black-player"), saved.getPlayerIds());
        assertEquals(session.getGame().getGameResult().status(), saved.getStatus());
        assertEquals(Color.BLACK, saved.getWinner());
        assertEquals("e2e4", saved.getMoves());
        assertEquals(1, saved.getPlyCount());
        assertEquals(0L, saved.getBaseSeconds());
        assertEquals(0L, saved.getIncrementSeconds());
        assertNull(saved.getClocksMs());
        assertEquals(session.getCreatedAt(), saved.getStartedAt());
        assertEquals(session.getEndedAt(), saved.getEndedAt());
        assertEquals(1, saved.getSchemaVersion());
        assertEquals(0, persister.pendingCount());
    }

    @Test
    @DisplayName("Should retry a transient repository failure and mark the session saved")
    void shouldRetryTransientFailureAndEventuallyMarkSaved() {
        GameSession session = createFinishedSession();
        AtomicInteger saveAttempts = new AtomicInteger();

        doAnswer(invocation -> {
            int attempt = saveAttempts.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("Temporary database failure");
            }
            return invocation.getArgument(0);
        }).when(repository).save(any(GameHistory.class));

        eventPublisher.publishEvent(new GameFinishedEvent(session));

        // Wait until the first attempt has failed and the retry has been queued.
        await().atMost(ASYNC_TIMEOUT).until(() -> persister.pendingCount() == 1);
        assertEquals(PersistState.PENDING, session.getPersistState());
        assertEquals(1, saveAttempts.get());

        // There is intentionally no scheduler yet. Drive the public retryDue()
        // method from the test; it will submit the retry once nextAt is reached.
        await()
                .pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofSeconds(6))
                .until(() -> {
                    persister.retryDue();
                    return session.isSaved();
                });

        assertEquals(2, saveAttempts.get());
        assertEquals(0, persister.pendingCount());
        verify(repository, times(2)).save(any(GameHistory.class));
    }

    @Test
    @DisplayName("Should mark the session failed without retrying a permanent error")
    void shouldNotRetryPermanentRepositoryFailure() {
        GameSession session = createFinishedSession();

        doThrow(new IllegalArgumentException("Invalid history document"))
                .when(repository).save(any(GameHistory.class));

        eventPublisher.publishEvent(new GameFinishedEvent(session));

        await().atMost(ASYNC_TIMEOUT)
                .until(() -> session.getPersistState() == PersistState.FAILED);

        verify(repository, times(1)).save(any(GameHistory.class));
        assertEquals(0, persister.pendingCount());
    }

    private GameSession createFinishedSession() {
        GameSession session = new GameSession(
                TimeControl.untimed(),
                "white-player",
                "black-player"
        );

        Move firstMove = new Move(new Position('e', 2), new Position('e', 4));
        assertTrue(session.getGame().move(firstMove).valid());
        session.addHistory(firstMove.toUCI());

        // End the game through resignation; GameService lifecycle behavior is
        // already covered by its own tests.
        assertTrue(session.getGame().resign(Color.WHITE));
        assertTrue(session.finalizeGameIfOver(Instant.now()));
        return session;
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        GameHistoryRepository gameHistoryRepository() {
            return mock(GameHistoryRepository.class);
        }

        @Bean
        GameHistoryMapper gameHistoryMapper() {
            return new GameHistoryMapper();
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        GameHistoryService gameHistoryService(GameHistoryRepository repository, GameHistoryMapper mapper, UserService userService) {
            return new GameHistoryService(repository, mapper, userService);
        }

        @Bean
        GameHistoryPersister gameHistoryPersister(GameHistoryService service) {
            return new GameHistoryPersister(service);
        }

        @Bean
        GameHistoryListener gameHistoryListener(
                GameHistoryMapper mapper,
                GameHistoryPersister persister
        ) {
            return new GameHistoryListener(mapper, persister);
        }
    }
}
