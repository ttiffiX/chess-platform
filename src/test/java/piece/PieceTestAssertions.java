package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.Piece;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PieceTestAssertions {

    private PieceTestAssertions() {
    }

    /**
     * Kiểm tra tập candidate destinations của một quân cờ.
     *
     * Không kiểm tra thứ tự vì thứ tự các ô được sinh ra
     * không ảnh hưởng đến luật di chuyển.
     *
     * Kiểm tra cả số lượng để phát hiện candidate bị trùng.
     */
    static void assertCandidates(
            Piece piece,
            Position from,
            Position... expected
    ) {
        List<Position> actual = piece.getCandidateDestinations(from);

        assertEquals(
                expected.length,
                actual.size(),
                "Candidate list has missing or duplicate positions"
        );

        assertEquals(
                new HashSet<>(Arrays.asList(expected)),
                new HashSet<>(actual),
                "Candidate destinations do not match expected positions"
        );

        assertFalse(
                actual.contains(from),
                "A piece cannot move to its current position"
        );
    }
}