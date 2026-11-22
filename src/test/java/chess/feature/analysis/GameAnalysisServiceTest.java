package chess.feature.analysis;

import chess.engine.model.game.GameResult;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.analysis.dto.GameReplayDto;
import chess.feature.analysis.service.GameAnalysisService;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.service.GameHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameAnalysisService")
class GameAnalysisServiceTest {

    private static final Instant STARTED_AT =
            Instant.parse("2026-11-20T10:00:00Z");

    private static final Instant ENDED_AT =
            Instant.parse("2026-11-20T10:15:00Z");

    // GameAnalysisService chỉ phụ thuộc GameHistoryService.
    // Việc "ai được xem ván" nằm trong GameHistoryService.getPlayerGame và được test ở GameHistoryServiceTest.
    @Mock
    private GameHistoryService gameHistoryService;

    @InjectMocks
    private GameAnalysisService gameAnalysisService;

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

            when(gameHistoryService.getPlayerGame(gameId, playerId))
                    .thenReturn(history);

            // Act
            GameReplayDto replay =
                    gameAnalysisService.getGameReplay(gameId, playerId);

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

            verify(gameHistoryService).getPlayerGame(gameId, playerId);
            verifyNoMoreInteractions(gameHistoryService);
        }

        @Test
        @DisplayName("Should propagate GAME_NOT_FOUND when the game is missing or the requester is not a participant")
        void shouldPropagateGameNotFound() {
            // Arrange: getPlayerGame ném lỗi cho cả hai trường hợp (không có ván / không phải người chơi).
            AppException notFound =
                    new AppException(ErrorCode.GAME_NOT_FOUND, "Game not found: game-1");

            when(gameHistoryService.getPlayerGame("game-1", "another-player"))
                    .thenThrow(notFound);

            // Act
            AppException exception = assertThrows(
                    AppException.class,
                    () -> gameAnalysisService.getGameReplay("game-1", "another-player")
            );

            // Assert
            assertSame(notFound, exception);
            assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());

            verify(gameHistoryService).getPlayerGame("game-1", "another-player");
            verifyNoMoreInteractions(gameHistoryService);
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

            when(gameHistoryService.getPlayerGame("game-1", "white-player"))
                    .thenReturn(history);

            // Act
            GameReplayDto replay =
                    gameAnalysisService.getGameReplay("game-1", "white-player");

            // Assert
            assertNotNull(replay);
            assertEquals(0, replay.plyCount());
            assertEquals(1, replay.records().size());

            assertNull(replay.records().getFirst().move());
            assertNull(replay.records().getFirst().moverClockAfterMs());
            assertFalse(replay.records().getFirst().legalMoves().isEmpty());
        }

        @Test
        @DisplayName("Should fail loudly when a stored move cannot be replayed")
        void shouldFailWhenStoredMoveIsIllegal() {
            // e2e5 không bao giờ hợp lệ từ vị trí đầu => dữ liệu lưu bị hỏng.
            GameHistory history = createHistory(
                    "game-1",
                    "e2e5",
                    1,
                    null
            );

            when(gameHistoryService.getPlayerGame("game-1", "white-player"))
                    .thenReturn(history);

            assertThrows(
                    IllegalStateException.class,
                    () -> gameAnalysisService.getGameReplay("game-1", "white-player")
            );
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