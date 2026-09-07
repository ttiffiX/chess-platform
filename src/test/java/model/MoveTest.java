package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    @Test
    void shouldCreateValidMove() {
        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        Move move = new Move(from, to);

        assertEquals(from, move.from());
        assertEquals(to, move.to());
    }

    @Test
    void shouldRejectNullFromPosition() {
        Position to = new Position('e', 4);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(null, to)
        );
    }

    @Test
    void shouldRejectNullToPosition() {
        Position from = new Position('e', 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(from, null)
        );
    }

    @Test
    void shouldRejectNullFromAndToPositions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(null, null)
        );
    }

    @Test
    void shouldRejectMoveWhenFromAndToAreSame() {
        Position position = new Position('e', 4);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Move(position, position)
        );
    }

    @Test
    void sameMovesShouldBeEqual() {
        Move firstMove = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        Move secondMove = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        assertEquals(firstMove, secondMove);
    }

    @Test
    void differentMovesShouldNotBeEqual() {
        Move firstMove = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        Move secondMove = new Move(
                new Position('e', 2),
                new Position('e', 5)
        );

        assertNotEquals(firstMove, secondMove);
    }

    @Test
    void equalMovesShouldHaveSameHashCode() {
        Move firstMove = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        Move secondMove = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        assertEquals(
                firstMove.hashCode(),
                secondMove.hashCode()
        );
    }

    @Test
    void shouldReturnCorrectFromPosition() {
        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        Move move = new Move(from, to);

        assertEquals(from, move.from());
    }

    @Test
    void shouldReturnCorrectToPosition() {
        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        Move move = new Move(from, to);

        assertEquals(to, move.to());
    }

    @Test
    void shouldReturnCorrectStringRepresentation() {
        Move move = new Move(
                new Position('e', 2),
                new Position('e', 4)
        );

        assertEquals("e2 -> e4", move.toString());
    }
}