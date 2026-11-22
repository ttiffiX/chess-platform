package chess.feature.gamehistory;

import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.mapper.GameHistoryMapper;
import chess.feature.gamehistory.model.GameOutcome;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import chess.feature.gamehistory.service.GameHistoryService;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import chess.feature.user.dto.PlayerSummary;
import chess.feature.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameHistoryService")
class GameHistoryServiceTest {

    private static final Instant STARTED_AT =
            Instant.parse("2026-11-20T10:00:00Z");

    private static final Instant ENDED_AT =
            Instant.parse("2026-11-20T10:15:00Z");

    @Mock
    private GameHistoryRepository gameHistoryRepository;

    @Mock
    private GameHistoryMapper gameHistoryMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameHistoryService gameHistoryService;


    // =========================================================
    // getGameHistory
    // =========================================================

    @Nested
    @DisplayName("getGameHistory")
    class GetGameHistoryTests {

        @Test
        @DisplayName("Should return paginated history and map opponent information and outcome")
        void shouldReturnPaginatedHistory() {
            // Arrange
            String playerId = "white-player";
            int pageNumber = 0;

            GameHistory history = createHistory(
                    "game-1",
                    "e2e4",
                    1,
                    null
            );

            PlayerSummary opponent = new PlayerSummary(
                    "black-player",
                    "Opponent",
                    "avatar-url"
            );

            GameHistoryDto expectedDto = new GameHistoryDto(
                    "game-1",
                    opponent,
                    Color.WHITE,
                    GameStatus.RESIGNED,
                    GameOutcome.WIN,
                    MatchmakingTimeControlPreset.BLITZ_3_2,
                    1,
                    STARTED_AT,
                    ENDED_AT
            );

            Pageable pageable = PageRequest.of(
                    pageNumber,
                    20,
                    Sort.by(Sort.Direction.DESC, "endedAt")
            );

            Page<GameHistory> historyPage = new PageImpl<>(
                    List.of(history),
                    pageable,
                    1
            );

            when(gameHistoryRepository.findByPlayerIdsContaining(
                    eq(playerId),
                    any(Pageable.class)
            )).thenReturn(historyPage);

            when(userService.getPlayerSummaries(List.of("black-player")))
                    .thenReturn(List.of(opponent));

            when(gameHistoryMapper.toGameHistoryDto(
                    history,
                    opponent,
                    Color.WHITE,
                    GameOutcome.WIN
            )).thenReturn(expectedDto);

            // Act
            Page<GameHistoryDto> result =
                    gameHistoryService.getGameHistory(playerId, pageNumber);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getNumberOfElements());
            assertEquals(1, result.getContent().size());
            assertEquals(expectedDto, result.getContent().getFirst());

            // Verify pagination and sorting are controlled by the service.
            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(Pageable.class);

            verify(gameHistoryRepository).findByPlayerIdsContaining(
                    eq(playerId),
                    pageableCaptor.capture()
            );

            Pageable actualPageable = pageableCaptor.getValue();

            assertEquals(0, actualPageable.getPageNumber());
            assertEquals(20, actualPageable.getPageSize());
            assertEquals(
                    Sort.Direction.DESC,
                    actualPageable.getSort().getOrderFor("endedAt").getDirection()
            );

            // Verify opponent information is batch-loaded.
            verify(userService).getPlayerSummaries(List.of("black-player"));

            // Verify the player is recognized as White and the result as a win.
            verify(gameHistoryMapper).toGameHistoryDto(
                    history,
                    opponent,
                    Color.WHITE,
                    GameOutcome.WIN
            );
        }

        @Test
        @DisplayName("Should reject a negative page number")
        void shouldRejectNegativePageNumber() {
            // Act
            AppException exception = assertThrows(
                    AppException.class,
                    () -> gameHistoryService.getGameHistory("white-player", -1)
            );

            // Assert
            assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());

            verifyNoInteractions(
                    gameHistoryRepository,
                    gameHistoryMapper,
                    userService
            );
        }

        @Test
        @DisplayName("Should use Unknown player when opponent information is unavailable")
        void shouldUseFallbackOpponentWhenUserDoesNotExist() {
            // Arrange
            String playerId = "white-player";

            GameHistory history = createHistory(
                    "game-1",
                    "e2e4",
                    1,
                    null
            );

            PlayerSummary unknownOpponent = new PlayerSummary(
                    "black-player",
                    "Unknown player",
                    null
            );

            GameHistoryDto expectedDto = new GameHistoryDto(
                    "game-1",
                    unknownOpponent,
                    Color.WHITE,
                    GameStatus.RESIGNED,
                    GameOutcome.WIN,
                    MatchmakingTimeControlPreset.BLITZ_3_2,
                    1,
                    STARTED_AT,
                    ENDED_AT
            );

            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(Sort.Direction.DESC, "endedAt")
            );

            when(gameHistoryRepository.findByPlayerIdsContaining(
                    eq(playerId),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(history), pageable, 1));

            // UserService không tìm thấy đối thủ.
            when(userService.getPlayerSummaries(List.of("black-player")))
                    .thenReturn(List.of());

            when(gameHistoryMapper.toGameHistoryDto(
                    history,
                    unknownOpponent,
                    Color.WHITE,
                    GameOutcome.WIN
            )).thenReturn(expectedDto);

            // Act
            Page<GameHistoryDto> result =
                    gameHistoryService.getGameHistory(playerId, 0);

            // Assert
            assertEquals(1, result.getContent().size());
            assertEquals(
                    "Unknown player",
                    result.getContent().getFirst().opponent().username()
            );

            verify(gameHistoryMapper).toGameHistoryDto(
                    history,
                    unknownOpponent,
                    Color.WHITE,
                    GameOutcome.WIN
            );
        }

        @Test
        @DisplayName("Should return an empty page without loading opponent information")
        void shouldReturnEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by(Sort.Direction.DESC, "endedAt")
            );

            when(gameHistoryRepository.findByPlayerIdsContaining(
                    eq("white-player"),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // Act
            Page<GameHistoryDto> result =
                    gameHistoryService.getGameHistory("white-player", 0);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(0, result.getTotalElements());

            // Không có lịch sử thì không cần gọi UserService hoặc Mapper.
            verifyNoInteractions(gameHistoryMapper, userService);
        }
    }


    // =========================================================
    // getPlayerGame
    // =========================================================

    @Nested
    @DisplayName("getPlayerGame")
    class GetPlayerGameTests {

        @Test
        @DisplayName("Should return the game when the requester is a participant")
        void shouldReturnGameForParticipant() {
            GameHistory history = createHistory("game-1", "e2e4", 1, null);

            when(gameHistoryRepository.findByGameIdAndPlayerIdsContaining("game-1", "white-player"))
                    .thenReturn(Optional.of(history));

            GameHistory result = gameHistoryService.getPlayerGame("game-1", "white-player");

            assertSame(history, result);
            verify(gameHistoryRepository).findByGameIdAndPlayerIdsContaining("game-1", "white-player");
            verifyNoInteractions(gameHistoryMapper, userService);
        }

        @Test
        @DisplayName("Should throw GAME_NOT_FOUND when no game matches the id and requester")
        void shouldThrowWhenNoMatch() {
            // Repository trả rỗng cho cả hai trường hợp: ván không tồn tại, hoặc người gọi không phải người chơi.
            // (Việc lọc theo playerIds do Mongo thực hiện nên không kiểm tra được bằng mock.)
            when(gameHistoryRepository.findByGameIdAndPlayerIdsContaining("game-1", "another-player"))
                    .thenReturn(Optional.empty());

            AppException exception = assertThrows(
                    AppException.class,
                    () -> gameHistoryService.getPlayerGame("game-1", "another-player")
            );

            assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());
            verify(gameHistoryRepository).findByGameIdAndPlayerIdsContaining("game-1", "another-player");
            verifyNoInteractions(gameHistoryMapper, userService);
        }
    }


    // =========================================================
    // Test data
    // =========================================================

    private static GameHistory createHistory(
            String gameId,
            String moves,
            int plyCount,
            long[] clocksMs
    ) {
        return GameHistory.builder()
                .gameId(gameId)
                .schemaVersion(1)
                .whitePlayerId("white-player")
                .blackPlayerId("black-player")
                .playerIds(List.of("white-player", "black-player"))
                .status(GameStatus.RESIGNED)
                .winner(Color.WHITE)
                .baseSeconds(180)
                .incrementSeconds(2)
                .moves(moves)
                .plyCount(plyCount)
                .clocksMs(clocksMs)
                .startedAt(STARTED_AT)
                .endedAt(ENDED_AT)
                .build();
    }
}