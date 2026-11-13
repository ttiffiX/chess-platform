package model.game;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.engine.rules.InvalidMoveReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            assertEquals(
                    GameStatus.IN_PROGRESS,
                    game.getGameResult().status()
            );

            assertFalse(game.isGameOver());

            assertEquals(32, game.getBoard().getPieces().size());

            assertNotNull(game.getBoard().getPiece(position("e1")));
            assertNotNull(game.getBoard().getPiece(position("e8")));

            assertNull(game.getCapturedPiece());
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

            String previousFEN = game.getCurrentFEN();

            var result = game.move(move("e7", "e5"));

            assertFalse(result.valid());
            assertEquals(
                    InvalidMoveReason.WRONG_TURN,
                    result.reason()
            );

            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(previousFEN, game.getCurrentFEN());

            assertNotNull(game.getBoard().getPiece(position("e7")));
            assertNull(game.getBoard().getPiece(position("e5")));
        }

        @Test
        @DisplayName("Should reject an illegal move without changing game state")
        void shouldRejectIllegalMove() {
            Game game = new Game();

            String previousFEN = game.getCurrentFEN();

            var result = game.move(move("e2", "e5"));

            assertFalse(result.valid());
            assertEquals(
                    InvalidMoveReason.ILLEGAL_PATTERN,
                    result.reason()
            );

            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(1, game.getFullMoveNumber());
            assertEquals(previousFEN, game.getCurrentFEN());

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
            assertEquals(
                    GameStatus.CHECKMATE,
                    game.getGameResult().status()
            );

            String previousFEN = game.getCurrentFEN();

            var result = game.move(move("e2", "e4"));

            assertFalse(result.valid());
            assertEquals(
                    InvalidMoveReason.GAME_ALREADY_OVER,
                    result.reason()
            );

            assertEquals(previousFEN, game.getCurrentFEN());
        }
    }

    @Nested
    @DisplayName("Captured Piece Tests")
    class CapturedPieceTests {

        @Test
        @DisplayName("Should expose captured piece as FEN-style symbol")
        void shouldExposeCapturedPieceFromLatestMove() {
            Game game = new Game();

            // White captures a black pawn.
            play(game, "e2", "e4");
            play(game, "d7", "d5");
            play(game, "e4", "d5");

            assertEquals("p", game.getCapturedPiece());

            // Black captures a white pawn.
            play(game, "d8", "d5");

            assertEquals("P", game.getCapturedPiece());

            // A non-capturing move should clear the latest captured piece.
            play(game, "g1", "f3");

            assertNull(game.getCapturedPiece());
        }

        @Test
        @DisplayName("Should not expose a captured piece after an invalid move")
        void shouldNotExposeCapturedPieceAfterInvalidMove() {
            Game game = new Game();

            var result = game.move(move("e2", "e5"));

            assertFalse(result.valid());
            assertNull(game.getCapturedPiece());
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
            assertEquals(
                    Color.BLACK,
                    game.getGameResult().winner()
            );
        }

        @Test
        @DisplayName("Should end game when black resigns")
        void shouldEndGameWhenBlackResigns() {
            Game game = new Game();

            assertTrue(game.resign(Color.BLACK));

            assertTrue(game.isGameOver());
            assertEquals(
                    Color.WHITE,
                    game.getGameResult().winner()
            );
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
            assertEquals(
                    Color.BLACK,
                    game.getGameResult().winner()
            );
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
                        + ", but got: " + result.reason()
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