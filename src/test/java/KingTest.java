package piece;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KingTest {

    private final King whiteKing = new King(Color.WHITE);

    @Test
    void shouldMoveOneSquareUp() {
        Position from = new Position('e', 4);
        Position to = new Position('e', 5);

        assertTrue(whiteKing.canMove(from, to));
    }

    @Test
    void shouldMoveOneSquareDown() {
        Position from = new Position('e', 4);
        Position to = new Position('e', 3);

        assertTrue(whiteKing.canMove(from, to));
    }

    @Test
    void shouldMoveOneSquareLeft() {
        Position from = new Position('e', 4);
        Position to = new Position('d', 4);

        assertTrue(whiteKing.canMove(from, to));
    }

    @Test
    void shouldMoveOneSquareRight() {
        Position from = new Position('e', 4);
        Position to = new Position('f', 4);

        assertTrue(whiteKing.canMove(from, to));
    }

    @Test
    void shouldMoveOneSquareDiagonally() {
        Position from = new Position('e', 4);

        assertTrue(
                whiteKing.canMove(
                        from,
                        new Position('f', 5)
                )
        );

        assertTrue(
                whiteKing.canMove(
                        from,
                        new Position('d', 5)
                )
        );

        assertTrue(
                whiteKing.canMove(
                        from,
                        new Position('f', 3)
                )
        );

        assertTrue(
                whiteKing.canMove(
                        from,
                        new Position('d', 3)
                )
        );
    }

    @Test
    void shouldNotMoveMoreThanOneSquare() {
        Position from = new Position('e', 4);

        assertFalse(
                whiteKing.canMove(
                        from,
                        new Position('e', 6)
                )
        );

        assertFalse(
                whiteKing.canMove(
                        from,
                        new Position('g', 4)
                )
        );

        assertFalse(
                whiteKing.canMove(
                        from,
                        new Position('g', 6)
                )
        );
    }

    @Test
    void shouldNotMoveToSamePosition() {
        Position from = new Position('e', 4);
        Position to = new Position('e', 4);

        assertFalse(whiteKing.canMove(from, to));
    }

    @Test
    void shouldNotMoveLikeKnight() {
        Position from = new Position('e', 4);

        assertFalse(
                whiteKing.canMove(
                        from,
                        new Position('f', 6)
                )
        );

        assertFalse(
                whiteKing.canMove(
                        from,
                        new Position('g', 5)
                )
        );
    }
}