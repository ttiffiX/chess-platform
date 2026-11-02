package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Queen Tests")
class QueenTest {

    private final Queen queen = new Queen(Color.WHITE);

    @Test
    @DisplayName("Should move horizontally, vertically and diagonally")
    void canMove_shouldAllowAllQueenDirections() {
        Position from = new Position('d', 4);

        // Queen kết hợp hai kiểu di chuyển:
        // ngang/dọc của Rook và đường chéo của Bishop.
        assertAll(
                () -> assertTrue(queen.canMove(from, new Position('h', 4))),
                () -> assertTrue(queen.canMove(from, new Position('d', 8))),
                () -> assertTrue(queen.canMove(from, new Position('h', 8))),
                () -> assertTrue(queen.canMove(from, new Position('a', 1)))
        );
    }

    @Test
    @DisplayName("Should reject moves that do not follow Queen directions")
    void canMove_shouldRejectInvalidDirections() {
        Position from = new Position('d', 4);

        // Queen không có khả năng di chuyển kiểu chữ L hoặc theo hướng bất kỳ.
        assertAll(
                () -> assertFalse(queen.canMove(from, new Position('e', 6))),
                () -> assertFalse(queen.canMove(from, new Position('f', 5))),
                () -> assertFalse(queen.canMove(from, new Position('d', 4)))
        );
    }

    @Test
    @DisplayName("Should capture using the same movement rules as Queen")
    void canCapture_shouldFollowMovementRules() {
        Position from = new Position('d', 4);

        assertAll(
                () -> assertTrue(queen.canCapture(from, new Position('h', 4))),
                () -> assertTrue(queen.canCapture(from, new Position('h', 8))),
                () -> assertFalse(queen.canCapture(from, new Position('e', 6)))
        );
    }

    @Test
    @DisplayName("Should return QUEEN as its piece type")
    void getType_shouldReturnQueen() {
        assertEquals(PieceType.QUEEN, queen.getType());
    }

    @Test
    @DisplayName("Should return twenty-seven candidate destinations from the center")
    void getCandidateDestinations_shouldReturnAllDirectionsFromCenter() {
        Position from = new Position('d', 4);

        // Rook đóng góp 14 ô ngang/dọc.
        // Bishop đóng góp 13 ô chéo.
        // Tổng cộng Queen có 27 candidate từ d4.
        PieceTestAssertions.assertCandidates(
                queen,
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
                new Position('d', 8),

                // Diagonal
                new Position('a', 1),
                new Position('b', 2),
                new Position('c', 3),
                new Position('e', 5),
                new Position('f', 6),
                new Position('g', 7),
                new Position('h', 8),
                new Position('a', 7),
                new Position('b', 6),
                new Position('c', 5),
                new Position('e', 3),
                new Position('f', 2),
                new Position('g', 1)
        );
    }

    @Test
    @DisplayName("Should return twenty-one candidate destinations from a corner")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        Position from = new Position('a', 1);

        // Từ a1:
        // 14 ô ngang/dọc + 7 ô chéo = 21.
        PieceTestAssertions.assertCandidates(
                queen,
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
                new Position('a', 8),

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