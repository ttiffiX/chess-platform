package model.game;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.Game;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Queen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game FEN Tests")
class GameFENTest {

    @Nested
    @DisplayName("Initial FEN Tests")
    class InitialFENTests {

        @Test
        @DisplayName("Should generate correct FEN for initial position")
        void shouldGenerateInitialFEN() {
            Game game = new Game();

            assertEquals(
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    game.getCurrentFEN()
            );
        }
    }

    @Nested
    @DisplayName("Piece Placement Tests")
    class PiecePlacementTests {

        @Test
        @DisplayName("Should generate correct piece placement after a pawn move")
        void shouldGeneratePiecePlacementAfterPawnMove() {
            Game game = new Game();

            play(game, "e2", "e4");

            assertEquals(
                    "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR",
                    game.getCurrentFEN().split(" ")[0]
            );
        }

        @Test
        @DisplayName("Should use uppercase for white pieces and lowercase for black pieces")
        void shouldUseCorrectPieceLetterCase() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "e7", "e5");

            String piecePlacement = game.getCurrentFEN().split(" ")[0];

            assertTrue(piecePlacement.contains("P"));
            assertTrue(piecePlacement.contains("p"));
        }
    }

    @Nested
    @DisplayName("Active Color Tests")
    class ActiveColorTests {

        @Test
        @DisplayName("Should indicate black to move after white moves")
        void shouldIndicateBlackToMove() {
            Game game = new Game();

            play(game, "e2", "e4");

            assertEquals("b", fenField(game, 1));
        }

        @Test
        @DisplayName("Should indicate white to move after black moves")
        void shouldIndicateWhiteToMove() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "e7", "e5");

            assertEquals("w", fenField(game, 1));
        }
    }

    @Nested
    @DisplayName("Castling Rights Tests")
    class CastlingRightsTests {

        @Test
        @DisplayName("Should include all castling rights in initial position")
        void shouldIncludeAllInitialCastlingRights() {
            Game game = new Game();

            assertEquals("KQkq", fenField(game, 2));
        }

        @Test
        @DisplayName("Should remove white castling rights after white king moves")
        void shouldRemoveWhiteCastlingRightsAfterKingMoves() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "a7", "a6");
            play(game, "e1", "e2");

            assertEquals("kq", fenField(game, 2));
        }

        @Test
        @DisplayName("Should remove corresponding castling right after rook moves")
        void shouldRemoveCorrespondingCastlingRightAfterRookMoves() {
            Game game = new Game();

            play(game, "h2", "h4");
            play(game, "a7", "a6");
            play(game, "h1", "h3");

            assertEquals("Qkq", fenField(game, 2));
        }
    }

    @Nested
    @DisplayName("En Passant Tests")
    class EnPassantTests {

        @Test
        @DisplayName("Should include en passant target after a two-square pawn move")
        void shouldIncludeEnPassantTargetAfterDoublePawnMove() {
            Game game = new Game();

            play(game, "e2", "e4");

            assertEquals("e3", fenField(game, 3));
        }

        @Test
        @DisplayName("Should include en passant target even when no capture is available")
        void shouldIncludeEnPassantTargetWithoutAdjacentPawn() {
            Game game = new Game();

            // No black pawn can capture on e3.
            play(game, "e2", "e4");

            assertEquals("e3", fenField(game, 3));
        }

        @Test
        @DisplayName("Should update en passant target after black double pawn move")
        void shouldUpdateEnPassantTargetAfterBlackDoublePawnMove() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "e7", "e5");

            assertEquals("e6", fenField(game, 3));
        }

        @Test
        @DisplayName("Should clear en passant target after the next move")
        void shouldClearEnPassantTargetAfterNextMove() {
            Game game = new Game();

            play(game, "e2", "e4");
            assertEquals("e3", fenField(game, 3));

            play(game, "a7", "a6");

            assertEquals("-", fenField(game, 3));
        }

        @Test
        @DisplayName("Should clear en passant target after an en passant capture")
        void shouldClearEnPassantTargetAfterCapture() {
            Game game = new Game();

            // 1. e4 a6 2. e5 d5
            play(game, "e2", "e4");
            play(game, "a7", "a6");
            play(game, "e4", "e5");
            play(game, "d7", "d5");

            assertEquals("d6", fenField(game, 3));

            // 3. exd6 e.p.
            play(game, "e5", "d6");

            assertEquals("-", fenField(game, 3));

            assertNotNull(game.getBoard().getPiece(position("d6")));
            assertNull(game.getBoard().getPiece(position("d5")));
        }
    }

    @Nested
    @DisplayName("Halfmove and Fullmove Tests")
    class MoveCounterTests {

        @Test
        @DisplayName("Should reset halfmove clock after a pawn move")
        void shouldResetHalfmoveClockAfterPawnMove() {
            Game game = new Game();

            play(game, "e2", "e4");

            assertEquals("0", fenField(game, 4));
        }

        @Test
        @DisplayName("Should increment halfmove clock after non-pawn moves")
        void shouldIncrementHalfmoveClockAfterNonPawnMoves() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "b8", "c6");
            play(game, "g1", "f3");

            assertEquals("2", fenField(game, 4));
        }

        @Test
        @DisplayName("Should reset halfmove clock after a capture")
        void shouldResetHalfmoveClockAfterCapture() {
            Game game = new Game();

            play(game, "e2", "e4");
            play(game, "d7", "d5");
            play(game, "e4", "d5");

            assertEquals("0", fenField(game, 4));
        }

        @Test
        @DisplayName("Should increment fullmove number after black moves")
        void shouldIncrementFullmoveNumberAfterBlackMoves() {
            Game game = new Game();

            assertEquals("1", fenField(game, 5));

            play(game, "e2", "e4");
            assertEquals("1", fenField(game, 5));

            play(game, "e7", "e5");
            assertEquals("2", fenField(game, 5));

            play(game, "g1", "f3");
            assertEquals("2", fenField(game, 5));
        }
    }

    @Nested
    @DisplayName("Promotion Tests")
    class PromotionTests {

        @Test
        @DisplayName("Should generate correct FEN after pawn promotion")
        void shouldGenerateCorrectFENAfterPromotion() {
            Game game = new Game();

            // Prepare a white pawn to capture the black rook on a8.
            play(game, "a2", "a4");
            play(game, "b7", "b5");
            play(game, "a4", "b5");
            play(game, "a7", "a6");
            play(game, "b5", "b6");
            play(game, "a6", "a5");
            play(game, "b6", "b7");
            play(game, "a5", "a4");

            Move promotionMove = new Move(
                    position("b7"),
                    position("a8"),
                    new Queen(Color.WHITE)
            );

            var result = game.move(promotionMove);

            assertTrue(result.valid());

            assertEquals(
                    "Qnbqkbnr/2pppppp/8/8/p7/8/1PPPPPPP/RNBQKBNR b KQk - 0 5",
                    game.getCurrentFEN()
            );
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

    private String fenField(Game game, int index) {
        return game.getCurrentFEN().split(" ")[index];
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