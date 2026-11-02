package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Knight Tests")
class KnightTest {

    private final Knight knight = new Knight(Color.WHITE);

    @Test
    @DisplayName("Should move in all eight L-shaped directions")
    void canMove_shouldAllowAllEightLShapedMoves() {
        Position from = new Position('d', 4);

        // Knight có thể di chuyển theo 8 hướng chữ L.
        // Mỗi nước đi có độ lệch (2,1) hoặc (1,2).
        assertAll(
                () -> assertTrue(knight.canMove(from, new Position('b', 3))),
                () -> assertTrue(knight.canMove(from, new Position('b', 5))),
                () -> assertTrue(knight.canMove(from, new Position('c', 2))),
                () -> assertTrue(knight.canMove(from, new Position('c', 6))),
                () -> assertTrue(knight.canMove(from, new Position('e', 2))),
                () -> assertTrue(knight.canMove(from, new Position('e', 6))),
                () -> assertTrue(knight.canMove(from, new Position('f', 3))),
                () -> assertTrue(knight.canMove(from, new Position('f', 5)))
        );
    }

    @Test
    @DisplayName("Should reject moves that do not form an L shape")
    void canMove_shouldRejectInvalidMoves() {
        Position from = new Position('d', 4);

        // Những nước đi này không có độ lệch (2,1) hoặc (1,2).
        assertAll(
                () -> assertFalse(knight.canMove(from, new Position('d', 5))),
                () -> assertFalse(knight.canMove(from, new Position('f', 6))),
                () -> assertFalse(knight.canMove(from, new Position('f', 4))),
                () -> assertFalse(knight.canMove(from, new Position('d', 4)))
        );
    }

    @Test
    @DisplayName("Should capture using the same L-shaped movement rules")
    void canCapture_shouldFollowMovementRules() {
        Position from = new Position('d', 4);

        assertAll(
                () -> assertTrue(knight.canCapture(from, new Position('f', 5))),
                () -> assertFalse(knight.canCapture(from, new Position('e', 5)))
        );
    }

    @Test
    @DisplayName("Should return KNIGHT as its piece type")
    void getType_shouldReturnKnight() {
        assertEquals(PieceType.KNIGHT, knight.getType());
    }

    @Test
    @DisplayName("Should return eight candidate destinations from the center")
    void getCandidateDestinations_shouldReturnEightPositionsFromCenter() {
        Position from = new Position('d', 4);

        PieceTestAssertions.assertCandidates(
                knight,
                from,
                new Position('b', 3),
                new Position('b', 5),
                new Position('c', 2),
                new Position('c', 6),
                new Position('e', 2),
                new Position('e', 6),
                new Position('f', 3),
                new Position('f', 5)
        );
    }

    @Test
    @DisplayName("Should return only two candidate destinations from a corner")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        Position from = new Position('a', 1);

        // Từ a1, chỉ có b3 và c2 nằm trong bàn cờ.
        PieceTestAssertions.assertCandidates(
                knight,
                from,
                new Position('b', 3),
                new Position('c', 2)
        );
    }
}