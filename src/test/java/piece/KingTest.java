package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("King Tests")
class KingTest {

    private final King king = new King(Color.WHITE);

    @Test
    @DisplayName("Should move one square in all eight directions")
    void canMove_shouldAllowOneSquareInAllDirections() {
        Position from = new Position('d', 4);

        // King được phép đi một ô theo ngang, dọc hoặc chéo.
        assertAll(
                () -> assertTrue(king.canMove(from, new Position('c', 3))),
                () -> assertTrue(king.canMove(from, new Position('d', 3))),
                () -> assertTrue(king.canMove(from, new Position('e', 3))),
                () -> assertTrue(king.canMove(from, new Position('c', 4))),
                () -> assertTrue(king.canMove(from, new Position('e', 4))),
                () -> assertTrue(king.canMove(from, new Position('c', 5))),
                () -> assertTrue(king.canMove(from, new Position('d', 5))),
                () -> assertTrue(king.canMove(from, new Position('e', 5)))
        );
    }

    @Test
    @DisplayName("Should reject moves that are more than one square")
    void canMove_shouldRejectMovesBeyondOneSquare() {
        Position from = new Position('d', 4);

        // King không phải quân trượt: không được đi xa hơn một ô.
        assertAll(
                () -> assertFalse(king.canMove(from, new Position('d', 6))),
                () -> assertFalse(king.canMove(from, new Position('f', 4))),
                () -> assertFalse(king.canMove(from, new Position('f', 6)))
        );
    }

    @Test
    @DisplayName("Should reject moving to the same position")
    void canMove_shouldRejectSamePosition() {
        Position from = new Position('d', 4);

        assertFalse(king.canMove(from, from));
    }

    @Test
    @DisplayName("Should capture using the same movement rules as King")
    void canCapture_shouldFollowMovementRules() {
        Position from = new Position('d', 4);

        // King bắt quân theo đúng phạm vi di chuyển một ô.
        assertAll(
                () -> assertTrue(king.canCapture(from, new Position('e', 5))),
                () -> assertFalse(king.canCapture(from, new Position('f', 6)))
        );
    }

    @Test
    @DisplayName("Should return KING as its piece type")
    void getType_shouldReturnKing() {
        assertEquals(PieceType.KING, king.getType());
    }

    @Test
    @DisplayName("Should return eight candidate destinations from the center")
    void getCandidateDestinations_shouldReturnEightPositionsFromCenter() {
        Position from = new Position('d', 4);

        PieceTestAssertions.assertCandidates(
                king,
                from,
                new Position('c', 3),
                new Position('d', 3),
                new Position('e', 3),
                new Position('c', 4),
                new Position('e', 4),
                new Position('c', 5),
                new Position('d', 5),
                new Position('e', 5)
        );
    }

    @Test
    @DisplayName("Should return only three candidate destinations from a corner")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        Position from = new Position('a', 1);

        // Ở góc a1, King chỉ có thể đến a2, b1 và b2.
        // Không được sinh ra các vị trí nằm ngoài bàn cờ.
        PieceTestAssertions.assertCandidates(
                king,
                from,
                new Position('a', 2),
                new Position('b', 1),
                new Position('b', 2)
        );
    }
}