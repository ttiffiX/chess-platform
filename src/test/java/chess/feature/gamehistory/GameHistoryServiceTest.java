package chess.feature.gamehistory;

import chess.engine.model.game.GameResult;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.dto.GameReplayDto;
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
    // getGameReplay
    // =========================================================

    @Nested
    @DisplayName("getGameReplay")
    class GetGameReplayTests {

        @Test
        @DisplayName("Should reconstruct all positions from stored UCI moves")
        void shouldReconstructCompleteGameReplay() {
            // Arrange
            String gameId = "game-1";
            String playerId = "white-player";

            /*
             * e2e4: White moves pawn
             * d7d5: Black moves pawn
             * e4d5: White captures Black's pawn
             */
            GameHistory history = createHistory(
                    gameId,
                    "e2e4 d7d5 e4d5",
                    3,
                    new long[]{
                            300_000,
                            295_000,
                            290_000
                    }
            );

            when(gameHistoryRepository.findById(gameId))
                    .thenReturn(Optional.of(history));

            // Act
            GameReplayDto replay =
                    gameHistoryService.getGameReplay(gameId, playerId);

            // Assert: game metadata
            assertNotNull(replay);
            assertEquals(gameId, replay.gameId());
            assertEquals("white-player", replay.whitePlayerId());
            assertEquals("black-player", replay.blackPlayerId());

            assertEquals(180L, replay.baseSeconds());
            assertEquals(2L, replay.incrementSeconds());
            assertEquals(3, replay.plyCount());

            // The persisted result is authoritative.
            assertEquals(
                    new GameResult(GameStatus.RESIGNED, Color.WHITE),
                    replay.gameResult()
            );

            assertEquals(STARTED_AT, replay.startedAt());
            assertEquals(ENDED_AT, replay.endedAt());

            // Assert: one initial position + three positions after moves.
            assertEquals(4, replay.records().size());

            var initialRecord = replay.records().get(0);
            var afterFirstMove = replay.records().get(1);
            var afterSecondMove = replay.records().get(2);
            var afterThirdMove = replay.records().get(3);

            // Initial position has no move.
            assertNull(initialRecord.move());
            assertTrue(initialRecord.fen().contains(" w "));
            assertNull(initialRecord.moverClockAfterMs());

            // Initial legal moves should contain e2-e4.
            assertTrue(initialRecord.legalMoves().get("e2").contains("e4"));

            // Position after e2e4.
            assertEquals("e2e4", afterFirstMove.move());
            assertTrue(afterFirstMove.fen().contains(" b "));
            assertEquals(
                    Integer.valueOf(300_000),
                    afterFirstMove.moverClockAfterMs()
            );

            // Position after d7d5.
            assertEquals("d7d5", afterSecondMove.move());
            assertTrue(afterSecondMove.fen().contains(" w "));
            assertEquals(
                    Integer.valueOf(295_000),
                    afterSecondMove.moverClockAfterMs()
            );

            // Position after e4d5: White captures a Black pawn.
            assertEquals("e4d5", afterThirdMove.move());
            assertEquals("p", afterThirdMove.capturedPiece());
            assertEquals(
                    Integer.valueOf(290_000),
                    afterThirdMove.moverClockAfterMs()
            );

            assertTrue(afterThirdMove.fen().contains(" b "));
            assertFalse(afterThirdMove.legalMoves().isEmpty());

            verify(gameHistoryRepository).findById(gameId);
            verifyNoInteractions(gameHistoryMapper, userService);
        }

        @Test
        @DisplayName("Should reject replay access when the requester is not a participant")
        void shouldRejectNonParticipant() {
            // Arrange
            GameHistory history = createHistory(
                    "game-1",
                    "e2e4",
                    1,
                    null
            );

            when(gameHistoryRepository.findById("game-1"))
                    .thenReturn(Optional.of(history));

            // Act
            AppException exception = assertThrows(
                    AppException.class,
                    () -> gameHistoryService.getGameReplay(
                            "game-1",
                            "another-player"
                    )
            );

            // Assert
            assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());

            verify(gameHistoryRepository).findById("game-1");
            verifyNoInteractions(gameHistoryMapper, userService);
        }

        @Test
        @DisplayName("Should throw GAME_NOT_FOUND when the history does not exist")
        void shouldThrowWhenHistoryDoesNotExist() {
            // Arrange
            when(gameHistoryRepository.findById("unknown-game"))
                    .thenReturn(Optional.empty());

            // Act
            AppException exception = assertThrows(
                    AppException.class,
                    () -> gameHistoryService.getGameReplay(
                            "unknown-game",
                            "white-player"
                    )
            );

            // Assert
            assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());

            verify(gameHistoryRepository).findById("unknown-game");
            verifyNoInteractions(gameHistoryMapper, userService);
        }

        @Test
        @DisplayName("Should return only the initial position when the game has no moves")
        void shouldHandleGameWithoutMoves() {
            // Arrange
            GameHistory history = createHistory(
                    "game-1",
                    "",
                    0,
                    null
            );

            when(gameHistoryRepository.findById("game-1"))
                    .thenReturn(Optional.of(history));

            // Act
            GameReplayDto replay =
                    gameHistoryService.getGameReplay("game-1", "white-player");

            // Assert
            assertNotNull(replay);
            assertEquals(0, replay.plyCount());
            assertEquals(1, replay.records().size());

            assertNull(replay.records().getFirst().move());
            assertNull(replay.records().getFirst().moverClockAfterMs());
            assertFalse(replay.records().getFirst().legalMoves().isEmpty());
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