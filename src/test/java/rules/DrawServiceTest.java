
package rules;

import chess.engine.model.Board;
import chess.engine.model.Position;
import chess.engine.model.piece.*;
import chess.engine.rules.services.DrawService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DrawServiceTest {

    private DrawService drawService;

    @BeforeEach
    void setUp() {
        drawService = new DrawService();
    }

    private Board createBoard(Object... entries) {
        Map<Position, Piece> pieces = new HashMap<>();

        for (int i = 0; i < entries.length; i += 2) {
            pieces.put((Position) entries[i], (Piece) entries[i + 1]);
        }

        return new Board(pieces);
    }

    private Position pos(char file, int rank) {
        return new Position(file, rank);
    }

    @Nested
    @DisplayName("Insufficient Material")
    class InsufficientMaterialTests {

        @Test
        @DisplayName("Vua vs Vua -> hòa")
        void shouldDrawWithOnlyKings() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK)
            );

            assertTrue(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Vua + Tượng vs Vua -> hòa")
        void shouldDrawWithKingAndBishopVsKing() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('c', 1), new Bishop(Color.WHITE)
            );

            assertTrue(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Vua + Mã vs Vua -> hòa")
        void shouldDrawWithKingAndKnightVsKing() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('b', 1), new Knight(Color.WHITE)
            );

            assertTrue(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Hai Tượng cùng màu ô -> hòa")
        void shouldDrawWithBishopsOnSameColorSquares() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('c', 1), new Bishop(Color.WHITE),
                    pos('f', 4), new Bishop(Color.BLACK)
            );

            assertTrue(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Hai Tượng khác màu ô -> không tự động hòa")
        void shouldNotDrawWithBishopsOnDifferentColorSquares() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('c', 1), new Bishop(Color.WHITE),
                    pos('f', 3), new Bishop(Color.BLACK)
            );

            assertFalse(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Còn một Xe -> không hòa")
        void shouldNotDrawWithRook() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('a', 1), new Rook(Color.WHITE)
            );

            assertFalse(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Còn một Hậu -> không hòa")
        void shouldNotDrawWithQueen() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('d', 1), new Queen(Color.WHITE)
            );

            assertFalse(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Còn một Tốt -> không hòa")
        void shouldNotDrawWithPawn() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('a', 2), new Pawn(Color.WHITE)
            );

            assertFalse(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Không hòa khi có nhiều hơn hai quân ngoài Vua")
        void shouldNotDrawWithMultiplePieces() {
            Board board = createBoard(
                    pos('e', 1), new King(Color.WHITE),
                    pos('e', 8), new King(Color.BLACK),
                    pos('c', 1), new Bishop(Color.WHITE),
                    pos('f', 3), new Bishop(Color.BLACK),
                    pos('a', 2), new Pawn(Color.WHITE)
            );

            assertFalse(drawService.isInsufficientMaterial(board));
        }

        @Test
        @DisplayName("Ném exception nếu Board null")
        void shouldThrowWhenBoardIsNull() {
            assertThrows(
                    NullPointerException.class,
                    () -> drawService.isInsufficientMaterial(null)
            );
        }
    }

    @Nested
    @DisplayName("Fifty Move Rule")
    class FiftyMoveRuleTests {

        @Test
        @DisplayName("Chưa đủ 100 half-moves -> không hòa")
        void shouldNotDrawBeforeOneHundredHalfMoves() {
            assertFalse(drawService.isFiftyMoveRule(0));
            assertFalse(drawService.isFiftyMoveRule(50));
            assertFalse(drawService.isFiftyMoveRule(99));
        }

        @Test
        @DisplayName("Đủ 100 half-moves -> hòa")
        void shouldDrawAtOneHundredHalfMoves() {
            assertTrue(drawService.isFiftyMoveRule(100));
        }

        @Test
        @DisplayName("Vượt 100 half-moves -> vẫn thỏa điều kiện hòa")
        void shouldDrawAfterOneHundredHalfMoves() {
            assertTrue(drawService.isFiftyMoveRule(101));
            assertTrue(drawService.isFiftyMoveRule(150));
        }
    }
}
