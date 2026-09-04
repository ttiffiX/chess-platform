import org.junit.jupiter.api.Test;
import piece.Color;
import piece.Position;
import piece.Rook;

import static org.junit.jupiter.api.Assertions.*;

class RookTest {

    private final Rook whiteRook = new Rook(Color.WHITE);

    @Test
    void shouldMoveVertically() {
        Position from = new Position('e', 4);

        assertTrue(
                whiteRook.canMove(from, new Position('e', 5))
        );

        assertTrue(
                whiteRook.canMove(from, new Position('e', 8))
        );

        assertTrue(
                whiteRook.canMove(from, new Position('e', 1))
        );
    }

    @Test
    void shouldMoveHorizontally() {
        Position from = new Position('e', 4);

        assertTrue(
                whiteRook.canMove(from, new Position('a', 4))
        );

        assertTrue(
                whiteRook.canMove(from, new Position('h', 4))
        );
    }

    @Test
    void shouldNotMoveDiagonally() {
        Position from = new Position('e', 4);

        assertFalse(
                whiteRook.canMove(from, new Position('f', 5))
        );

        assertFalse(
                whiteRook.canMove(from, new Position('d', 3))
        );

        assertFalse(
                whiteRook.canMove(from, new Position('g', 6))
        );
    }

    @Test
    void shouldNotMoveToSamePosition() {
        Position position = new Position('e', 4);

        assertFalse(
                whiteRook.canMove(position, position)
        );
    }

    @Test
    void shouldMoveFromCorner() {
        Position from = new Position('a', 1);

        assertTrue(
                whiteRook.canMove(from, new Position('a', 8))
        );

        assertTrue(
                whiteRook.canMove(from, new Position('h', 1))
        );

        assertFalse(
                whiteRook.canMove(from, new Position('b', 2))
        );
    }
}