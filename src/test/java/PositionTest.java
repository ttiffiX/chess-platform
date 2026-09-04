import org.junit.jupiter.api.Test;
import piece.Position;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test
    void testPositionCreation() {
        Position position = new Position('e', 4);

        assertEquals('e', position.file());
        assertEquals(4, position.rank());
    }

    @Test
    void testInvalidPositionCreation() {
        assertThrows(IllegalArgumentException.class, () -> new Position('i', 5));
        assertThrows(IllegalArgumentException.class, () -> new Position('d', 9));
        assertThrows(IllegalArgumentException.class, () -> new Position('z', 0));
    }

    @Test
    void testPositionEquality() {
        Position position1 = new Position('c', 4);
        Position position2 = new Position('c', 4);
        Position position3 = new Position('e', 6);

        assertEquals(position1, position2);
        assertNotEquals(position1, position3);
    }

    @Test
    void testPositionHashCode() {
        Position position1 = new Position('b', 2);
        Position position2 = new Position('b', 2);
        Position position3 = new Position('f', 5);

        assertEquals(position1.hashCode(), position2.hashCode());
        assertNotEquals(position1.hashCode(), position3.hashCode());
    }

    @Test
    void testPositionToString() {
        Position position = new Position('g', 3);

        assertEquals("g3", position.toString());
    }

}
