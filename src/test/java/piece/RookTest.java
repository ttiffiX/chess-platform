package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rook Tests")
class RookTest {

    private final Rook rook = new Rook(Color.WHITE);

    @Test
    @DisplayName("Should move horizontally and vertically")
    void canMove_shouldAllowHorizontalAndVerticalMoves() {
        Position from = new Position('d', 4);

        // Rook có thể di chuyển nhiều ô theo cùng một hàng hoặc cột.
        assertAll(
                () -> assertTrue(rook.canMove(from, new Position('a', 4))),
                () -> assertTrue(rook.canMove(from, new Position('h', 4))),
                () -> assertTrue(rook.canMove(from, new Position('d', 1))),
                () -> assertTrue(rook.canMove(from, new Position('d', 8)))
        );
    }

    @Test
    @DisplayName("Should reject diagonal and irregular moves")
    void canMove_shouldRejectInvalidDirections() {
        Position from = new Position('d', 4);

        // Rook không được đi chéo hoặc di chuyển theo hướng không thẳng hàng.
        assertAll(
                () -> assertFalse(rook.canMove(from, new Position('e', 5))),
                () -> assertFalse(rook.canMove(from, new Position('f', 5))),
                () -> assertFalse(rook.canMove(from, new Position('d', 4)))
        );
    }

    @Test
    @DisplayName("Should capture using the same movement rules as Rook")
    void canCapture_shouldFollowMovementRules() {
        Position from = new Position('d', 4);

        assertAll(
                () -> assertTrue(rook.canCapture(from, new Position('d', 8))),
                () -> assertTrue(rook.canCapture(from, new Position('a', 4))),
                () -> assertFalse(rook.canCapture(from, new Position('e', 5)))
        );
    }

    @Test
    @DisplayName("Should return ROOK as its piece type")
    void getType_shouldReturnRook() {
        assertEquals(PieceType.ROOK, rook.getType());
    }

    @Test
    @DisplayName("Should return fourteen candidate destinations from the center")
    void getCandidateDestinations_shouldReturnFourteenPositionsFromCenter() {
        Position from = new Position('d', 4);

        PieceTestAssertions.assertCandidates(
                rook,
                from,
                // Horizontal
                new Position('a', 4),
                new Position('b', 4),
                new Position('c', 4),
                new Position('e', 4),
                new Position('f', 4),
                new Position('g', 4),
                new Position('h', 4),

                // Vertical
                new Position('d', 1),
                new Position('d', 2),
                new Position('d', 3),
                new Position('d', 5),
                new Position('d', 6),
                new Position('d', 7),
                new Position('d', 8)
        );
    }

    @Test
    @DisplayName("Should return fourteen candidate destinations from a corner")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        Position from = new Position('a', 1);

        // Từ góc a1, Rook có 7 ô trên hàng ngang và 7 ô trên cột dọc.
        PieceTestAssertions.assertCandidates(
                rook,
                from,
                new Position('b', 1),
                new Position('c', 1),
                new Position('d', 1),
                new Position('e', 1),
                new Position('f', 1),
                new Position('g', 1),
                new Position('h', 1),

                new Position('a', 2),
                new Position('a', 3),
                new Position('a', 4),
                new Position('a', 5),
                new Position('a', 6),
                new Position('a', 7),
                new Position('a', 8)
        );
    }
}