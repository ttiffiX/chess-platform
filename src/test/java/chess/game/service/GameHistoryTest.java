package chess.game.service;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.PieceType;
import chess.feature.game.model.Record;
import chess.feature.game.model.TimeControl;
import chess.feature.game.session.GameSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game Session History Tests")
class GameHistoryTest {

    private static final String WHITE_PLAYER_ID = "white-player";
    private static final String BLACK_PLAYER_ID = "black-player";

    @Nested
    @DisplayName("Initial History Tests")
    class InitialHistoryTests {

        @Test
        @DisplayName("Should record initial game state")
        void shouldRecordInitialGameState() {
            GameSession session = createSession();

            List<Record> history = session.getHistory();

            assertEquals(1, history.size());

            Record initialRecord = history.getFirst();

            assertNull(initialRecord.move());
            assertEquals(
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    initialRecord.fen()
            );
            assertNull(initialRecord.capturedPiece());
            assertFalse(initialRecord.inCheck());
            assertNull(initialRecord.moverClockAfterMs());
        }
    }

    @Nested
    @DisplayName("Move History Tests")
    class MoveHistoryTests {

        @Test
        @DisplayName("Should record each successfully completed move")
        void shouldRecordSuccessfulMoves() {
            GameSession session = createSession();

            playAndRecord(session, "e2", "e4");
            playAndRecord(session, "e7", "e5");
            playAndRecord(session, "g1", "f3");

            List<Record> history = session.getHistory();

            assertEquals(4, history.size());

            Record firstMove = history.get(1);

            assertEquals(move("e2", "e4"), firstMove.move());
            assertEquals(
                    "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                    firstMove.fen()
            );

            Record thirdMove = history.get(3);

            assertEquals(move("g1", "f3"), thirdMove.move());
            assertFalse(thirdMove.inCheck());
        }

        @Test
        @DisplayName("Should store captured piece in the corresponding history record")
        void shouldRecordCapturedPiece() {
            GameSession session = createSession();

            playAndRecord(session, "e2", "e4");
            playAndRecord(session, "d7", "d5");
            playAndRecord(session, "e4", "d5");

            Record whiteCapture = session.getHistory().get(3);

            assertNotNull(whiteCapture.capturedPiece());
            assertEquals(
                    PieceType.PAWN,
                    whiteCapture.capturedPiece().getType()
            );
            assertEquals(
                    Color.BLACK,
                    whiteCapture.capturedPiece().getColor()
            );

            playAndRecord(session, "d8", "d5");

            Record blackCapture = session.getHistory().get(4);

            assertNotNull(blackCapture.capturedPiece());
            assertEquals(
                    PieceType.PAWN,
                    blackCapture.capturedPiece().getType()
            );
            assertEquals(
                    Color.WHITE,
                    blackCapture.capturedPiece().getColor()
            );
        }

        @Test
        @DisplayName("Should not record an invalid move")
        void shouldNotRecordInvalidMove() {
            GameSession session = createSession();

            var result = session.getGame().move(move("e2", "e5"));

            assertFalse(result.valid());
            assertEquals(1, session.getHistory().size());
        }

        @Test
        @DisplayName("Should record check state after a checking move")
        void shouldRecordCheckStateAfterCheckingMove() {
            GameSession session = createSession();

            // Fool's Mate
            playAndRecord(session, "f2", "f3");
            playAndRecord(session, "e7", "e5");
            playAndRecord(session, "g2", "g4");
            playAndRecord(session, "d8", "h4");

            Record finalRecord = session.getLastHistory();

            assertTrue(finalRecord.inCheck());
            assertEquals(Color.WHITE, session.getGame().getCurrentTurn());
        }
    }

    @Nested
    @DisplayName("Clock History Tests")
    class ClockHistoryTests {

        @Test
        @DisplayName("Should record mover remaining time after completing a move")
        void shouldRecordMoverRemainingTime() {
            GameSession session = new GameSession(
                    new TimeControl(60, 2),
                    WHITE_PLAYER_ID,
                    BLACK_PLAYER_ID
            );

            Instant start = Instant.parse("2026-01-01T10:00:00Z");
            Instant moveReceivedAt = start.plusMillis(1_000);

            session.startClock(start);

            Move move = move("e2", "e4");

            var result = session.getGame().move(move);
            assertTrue(result.valid());

            session.completeMove(
                    Color.WHITE,
                    moveReceivedAt,
                    moveReceivedAt
            );

            session.addHistory(move);

            Record record = session.getLastHistory();

            // 60,000 - 1,000 elapsed + 2,000 increment
            assertEquals(61_000, record.moverClockAfterMs());
        }

        @Test
        @DisplayName("Should store null mover clock for untimed games")
        void shouldStoreNullClockForUntimedGame() {
            GameSession session = createSession();

            playAndRecord(session, "e2", "e4");

            assertNull(session.getLastHistory().moverClockAfterMs());
        }
    }

    @Nested
    @DisplayName("History Encapsulation Tests")
    class HistoryEncapsulationTests {

        @Test
        @DisplayName("Should not expose the internal history list")
        void shouldReturnDefensiveHistoryList() {
            GameSession session = createSession();

            List<Record> history = session.getHistory();
            history.clear();

            assertEquals(1, session.getHistory().size());
        }
    }

    private GameSession createSession() {
        return new GameSession(
                TimeControl.untimed(),
                WHITE_PLAYER_ID,
                BLACK_PLAYER_ID
        );
    }

    private void playAndRecord(
            GameSession session,
            String from,
            String to
    ) {
        Move move = move(from, to);

        var result = session.getGame().move(move);

        assertTrue(
                result.valid(),
                () -> "Expected legal move: " + move
        );

        session.addHistory(move);
    }

    private Move move(String from, String to) {
        return new Move(position(from), position(to));
    }

    private Position position(String square) {
        return new Position(
                square.charAt(0),
                Character.getNumericValue(square.charAt(1))
        );
    }
}