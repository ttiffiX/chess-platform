package model.game;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Pawn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game History Tests")
class GameHistoryTest {

    @Nested
    @DisplayName("Initial History Tests")
    class InitialHistoryTests {

        @Test
        @DisplayName("Should record initial game state")
        void shouldRecordInitialGameState() {
            Game game = new Game();

            List<GameStateSnapshot> history = game.getGameStateHistory();

            assertEquals(1, history.size());

            GameStateSnapshot initialSnapshot = history.getFirst();

            assertNull(initialSnapshot.lastMove());
            assertNull(initialSnapshot.capturedPiece());
            assertEquals(Color.WHITE, initialSnapshot.turn());
            assertEquals(1, initialSnapshot.fullMoveNumber());
            assertEquals(0, initialSnapshot.halfMoveClock());
            assertFalse(initialSnapshot.inCheck());
            assertEquals(GameStatus.IN_PROGRESS, initialSnapshot.gameResult().status());

            assertNotNull(initialSnapshot.board().getPiece(new Position('e', 1)));
            assertNotNull(initialSnapshot.board().getPiece(new Position('e', 8)));
        }
    }

    @Nested
    @DisplayName("Move History Tests")
    class MoveHistoryTests {

        @Test
        @DisplayName("Should record the complete state after a normal move")
        void shouldRecordCompleteStateAfterMove() {
            Game game = new Game();

            Move move = new Move(
                    new Position('e', 2),
                    new Position('e', 4)
            );

            assertTrue(game.move(move).valid());

            GameStateSnapshot snapshot = game.getSnapshotAt(1);

            assertEquals(move, snapshot.lastMove());
            assertEquals(Color.BLACK, snapshot.turn());
            assertEquals(0, snapshot.halfMoveClock());
            assertEquals(1, snapshot.fullMoveNumber());
            assertFalse(snapshot.inCheck());
            assertEquals(GameStatus.IN_PROGRESS, snapshot.gameResult().status());

            assertNull(snapshot.board().getPiece(new Position('e', 2)));
            assertInstanceOf(Pawn.class, snapshot.board().getPiece(new Position('e', 4)));
        }

        @Test
        @DisplayName("Should record the captured piece in history")
        void shouldRecordCapturedPiece() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "d7", "d5");
            play(game, "e4", "d5");

            GameStateSnapshot snapshot = game.getSnapshotAt(3);

            assertNotNull(snapshot.capturedPiece());
            assertInstanceOf(Pawn.class, snapshot.capturedPiece());
            assertEquals(Color.BLACK, snapshot.capturedPiece().getColor());

            assertEquals(1, game.getCapturedPieces().size());
            assertEquals(1, game.getCapturedPiecesByColor(Color.BLACK).size());
            assertTrue(game.getCapturedPiecesByColor(Color.WHITE).isEmpty());
        }

        @Test
        @DisplayName("Should record check state after a checking move")
        void shouldRecordCheckStateAfterCheckingMove() {
            Game game = new Game();

            // Scholar's Mate setup: the queen delivers checkmate on f7.
            play(game, "e2", "e4");
            play(game, "e7", "e5");
            play(game, "f1", "c4");
            play(game, "b8", "c6");
            play(game, "d1", "h5");
            play(game, "g8", "f6");

            // Check is delivered, but the game is not over yet.
            // Use a different checking move if the position permits an escape.
            play(game, "h5", "f7");

            GameStateSnapshot snapshot = game.getSnapshotAt(
                    game.getGameStateHistory().size() - 1
            );

            assertTrue(snapshot.inCheck());
            assertEquals(Color.BLACK, snapshot.turn());
            assertEquals(GameStatus.CHECKMATE, snapshot.gameResult().status());
        }
    }

    @Nested
    @DisplayName("History Encapsulation Tests")
    class HistoryEncapsulationTests {

        @Test
        @DisplayName("Should not expose the internal history list")
        void shouldReturnDefensiveHistoryList() {
            Game game = new Game();

            List<GameStateSnapshot> history = game.getGameStateHistory();
            history.clear();

            assertEquals(1, game.getGameStateHistory().size());
        }

        @Test
        @DisplayName("Should not allow external changes to a historical board")
        void shouldProtectHistoricalBoardFromExternalChanges() {
            Game game = new Game();

            GameStateSnapshot initialSnapshot = game.getSnapshotAt(0);

            // Mutating a retrieved board must not mutate the stored snapshot.
            initialSnapshot.board().removePiece(new Position('e', 1));

            assertNotNull(
                    initialSnapshot.board().getPiece(new Position('e', 1))
            );
        }
    }

    private void play(Game game, String from, String to) {
        Move move = new Move(
                toPosition(from),
                toPosition(to)
        );

        assertTrue(
                game.move(move).valid(),
                () -> "Expected legal move: " + move
        );
    }

    private Position toPosition(String square) {
        return new Position(
                square.charAt(0),
                Character.getNumericValue(square.charAt(1))
        );
    }
}