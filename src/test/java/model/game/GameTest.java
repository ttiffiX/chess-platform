
package model.game;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.engine.rules.InvalidMoveReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game Tests")
class GameTest {

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize game with correct initial state")
        void shouldInitializeCorrectly() {
            Game game = new Game();

            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertFalse(game.isInCheck());
            assertEquals(1, game.getFullMoveNumber());
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
            assertFalse(game.isGameOver());

            assertEquals(1, game.getGameStateHistory().size());
            assertTrue(game.getCapturedPieces().isEmpty());

            GameStateSnapshot initialSnapshot = game.getSnapshotAt(0);

            assertNull(initialSnapshot.lastMove());
            assertEquals(Color.WHITE, initialSnapshot.turn());
            assertEquals(1, initialSnapshot.fullMoveNumber());
            assertEquals(0, initialSnapshot.halfMoveClock());
        }
    }

    @Nested
    @DisplayName("Move Lifecycle Tests")
    class MoveLifecycleTests {

        @Test
        @DisplayName("Should alternate turns after each valid move")
        void shouldAlternateTurns() {
            Game game = new Game();

            play(game, "e2", "e4");
            assertEquals(Color.BLACK, game.getCurrentTurn());

            play(game, "e7", "e5");
            assertEquals(Color.WHITE, game.getCurrentTurn());
        }

        @Test
        @DisplayName("Should update full move number after black moves")
        void shouldUpdateFullMoveNumber() {
            Game game = new Game();

            play(game, "e2", "e4");
            assertEquals(1, game.getFullMoveNumber());

            play(game, "e7", "e5");
            assertEquals(2, game.getFullMoveNumber());

            play(game, "g1", "f3");
            assertEquals(2, game.getFullMoveNumber());

            play(game, "b8", "c6");
            assertEquals(3, game.getFullMoveNumber());
        }

        @Test
        @DisplayName("Should reject moving a piece belonging to the opponent")
        void shouldRejectWrongTurn() {
            Game game = new Game();

            var result = game.move(move("e7", "e5"));

            assertFalse(result.valid());
            assertEquals(InvalidMoveReason.WRONG_TURN, result.reason());
            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(1, game.getGameStateHistory().size());
        }

        @Test
        @DisplayName("Should reject an illegal move without changing game state")
        void shouldRejectIllegalMove() {
            Game game = new Game();

            var result = game.move(move("e2", "e5"));

            assertFalse(result.valid());
            assertEquals(InvalidMoveReason.ILLEGAL_PATTERN, result.reason());

            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(1, game.getFullMoveNumber());
            assertEquals(1, game.getGameStateHistory().size());

            assertNotNull(game.getBoard().getPiece(position("e2")));
            assertNull(game.getBoard().getPiece(position("e5")));
        }

        @Test
        @DisplayName("Should reject further moves after game is over")
        void shouldRejectMoveWhenGameIsOver() {
            Game game = new Game();

            // Fool's Mate
            play(game, "f2", "f3");
            play(game, "e7", "e5");
            play(game, "g2", "g4");
            play(game, "d8", "h4");

            assertTrue(game.isGameOver());

            int historySize = game.getGameStateHistory().size();

            var result = game.move(move("e2", "e4"));

            assertFalse(result.valid());
            assertEquals(InvalidMoveReason.GAME_ALREADY_OVER, result.reason());
            assertEquals(historySize, game.getGameStateHistory().size());
        }
    }

    @Nested
    @DisplayName("Game History Tests")
    class GameHistoryTests {

        @Test
        @DisplayName("Should record each successfully completed move")
        void shouldRecordSuccessfulMoves() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "e7", "e5");
            play(game, "g1", "f3");

            List<GameStateSnapshot> history = game.getGameStateHistory();

            assertEquals(4, history.size());

            GameStateSnapshot firstMove = history.get(1);
            assertEquals(move("e2", "e4"), firstMove.lastMove());
            assertEquals(Color.BLACK, firstMove.turn());

            GameStateSnapshot thirdMove = history.get(3);
            assertEquals(move("g1", "f3"), thirdMove.lastMove());
            assertEquals(Color.BLACK, thirdMove.turn());
        }

        @Test
        @DisplayName("Should not record an invalid move")
        void shouldNotRecordInvalidMove() {
            Game game = new Game();

            game.move(move("e2", "e5"));

            assertEquals(1, game.getGameStateHistory().size());
            assertNull(game.getSnapshotAt(0).lastMove());
        }

        @Test
        @DisplayName("Should reject an out-of-range snapshot index")
        void shouldRejectInvalidSnapshotIndex() {
            Game game = new Game();

            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> game.getSnapshotAt(-1)
            );

            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> game.getSnapshotAt(1)
            );
        }
    }

    @Nested
    @DisplayName("Captured Pieces Tests")
    class CapturedPiecesTests {

        @Test
        @DisplayName("Should track captured pieces by their original color")
        void shouldTrackCapturedPiecesByColor() {
            Game game = new Game();

            // White captures a black pawn, then black captures a white pawn.
            play(game, "e2", "e4");
            play(game, "d7", "d5");
            play(game, "e4", "d5");
            play(game, "d8", "d5");

            List<chess.engine.model.piece.Piece> capturedPieces =
                    game.getCapturedPieces();

            assertEquals(2, capturedPieces.size());
            assertEquals(1, game.getCapturedPiecesByColor(Color.WHITE).size());
            assertEquals(1, game.getCapturedPiecesByColor(Color.BLACK).size());

            assertEquals(
                    Color.BLACK,
                    game.getCapturedPiecesByColor(Color.BLACK).getFirst().getColor()
            );

            assertEquals(
                    Color.WHITE,
                    game.getCapturedPiecesByColor(Color.WHITE).getFirst().getColor()
            );
        }

        @Test
        @DisplayName("Should not track pieces from an invalid capture")
        void shouldNotTrackInvalidCapture() {
            Game game = new Game();

            game.move(move("e2", "e5"));

            assertTrue(game.getCapturedPieces().isEmpty());
        }
    }

    @Nested
    @DisplayName("Game Ending Tests")
    class GameEndingTests {

        @Test
        @DisplayName("Should end game when white resigns")
        void shouldEndGameWhenWhiteResigns() {
            Game game = new Game();

            assertTrue(game.resign(Color.WHITE));

            assertTrue(game.isGameOver());
            assertEquals(Color.BLACK, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Should end game when black resigns")
        void shouldEndGameWhenBlackResigns() {
            Game game = new Game();

            assertTrue(game.resign(Color.BLACK));

            assertTrue(game.isGameOver());
            assertEquals(Color.WHITE, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Should end game by agreement draw")
        void shouldAgreeDraw() {
            Game game = new Game();

            assertTrue(game.agreeDraw());

            assertTrue(game.isGameOver());
            assertEquals(
                    GameStatus.DRAW_BY_AGREEMENT,
                    game.getGameResult().status()
            );
        }

        @Test
        @DisplayName("Should end game when a player runs out of time")
        void shouldEndGameOnTimeout() {
            Game game = new Game();

            assertTrue(game.isTimedOut(Color.WHITE));

            assertTrue(game.isGameOver());
            assertEquals(Color.BLACK, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Should not allow ending an already finished game")
        void shouldRejectFurtherEndingActions() {
            Game game = new Game();

            assertTrue(game.resign(Color.WHITE));

            assertFalse(game.resign(Color.BLACK));
            assertFalse(game.agreeDraw());
            assertFalse(game.isTimedOut(Color.BLACK));
        }
    }

    private void play(Game game, String from, String to) {
        var result = game.move(move(from, to));

        assertTrue(
                result.valid(),
                () -> "Expected legal move: " + from + " -> " + to
        );
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
