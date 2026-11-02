package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bishop Tests")
class BishopTest {

    private final Bishop bishop = new Bishop(Color.WHITE);

    @Test
    @DisplayName("Should move diagonally in all four directions")
    void canMove_shouldAllowDiagonalMoves() {
        Position from = new Position('d', 4);

        // Bishop có thể đi theo bốn đường chéo:
        // lên-phải, lên-trái, xuống-phải và xuống-trái.
        assertAll(
                () -> assertTrue(bishop.canMove(from, new Position('h', 8))),
                () -> assertTrue(bishop.canMove(from, new Position('a', 7))),
                () -> assertTrue(bishop.canMove(from, new Position('g', 1))),
                () -> assertTrue(bishop.canMove(from, new Position('a', 1)))
        );
    }

    @Test
    @DisplayName("Should reject horizontal, vertical and irregular moves")
    void canMove_shouldRejectNonDiagonalMoves() {
        Position from = new Position('d', 4);

        // Bishop không được di chuyển ngang, dọc hoặc theo một hướng bất kỳ.
        assertAll(
                () -> assertFalse(bishop.canMove(from, new Position('d', 8))),
                () -> assertFalse(bishop.canMove(from, new Position('h', 4))),
                () -> assertFalse(bishop.canMove(from, new Position('e', 6))),
                () -> assertFalse(bishop.canMove(from, new Position('d', 4)))
        );
    }

    @Test
    @DisplayName("Should capture using the same movement rules as Bishop")
    void canCapture_shouldFollowMovementRules() {
        Position from = new Position('d', 4);

        assertAll(
                () -> assertTrue(bishop.canCapture(from, new Position('h', 8))),
                () -> assertTrue(bishop.canCapture(from, new Position('a', 1))),
                () -> assertFalse(bishop.canCapture(from, new Position('d', 8)))
        );
    }

    @Test
    @DisplayName("Should return BISHOP as its piece type")
    void getType_shouldReturnBishop() {
        assertEquals(PieceType.BISHOP, bishop.getType());
    }

    @Test
    @DisplayName("Should return thirteen candidate destinations from the center")
    void getCandidateDestinations_shouldReturnAllDiagonalPositionsFromCenter() {
        Position from = new Position('d', 4);

        // Từ d4 có tổng cộng 13 ô trên bốn đường chéo.
        PieceTestAssertions.assertCandidates(
                bishop,
                from,
                // Up-right
                new Position('e', 5),
                new Position('f', 6),
                new Position('g', 7),
                new Position('h', 8),

                // Up-left
                new Position('c', 5),
                new Position('b', 6),
                new Position('a', 7),

                // Down-right
                new Position('e', 3),
                new Position('f', 2),
                new Position('g', 1),

                // Down-left
                new Position('c', 3),
                new Position('b', 2),
                new Position('a', 1)
        );
    }

    @Test
    @DisplayName("Should return seven candidate destinations from a corner")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        Position from = new Position('a', 1);

        // Từ a1 chỉ có một đường chéo đi vào bên trong bàn cờ.
        PieceTestAssertions.assertCandidates(
                bishop,
                from,
                new Position('b', 2),
                new Position('c', 3),
                new Position('d', 4),
                new Position('e', 5),
                new Position('f', 6),
                new Position('g', 7),
                new Position('h', 8)
        );
    }
}