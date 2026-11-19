package chess.engine.rules;

import chess.engine.model.Move;
import chess.engine.model.game.Game;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerftTest {
    private static long perft(List<Move> path, int depth) {
        Game g = new Game();
        for (Move m : path) assertTrue(g.move(m).valid());
        List<Move> moves = List.copyOf(g.getAllLegalMovesByColor());
        if (depth == 1) return moves.size();
        long n = 0;
        for (Move mv : moves) {
            List<Move> next = new ArrayList<>(path);
            next.add(mv);
            n += perft(next, depth - 1);
        }
        return n;
    }

    @ParameterizedTest
    @CsvSource({"1,20", "2,400", "3,8902", "4,197281"})
    void startPosition(int depth, long expected) {
        assertEquals(expected, perft(new ArrayList<>(), depth));
    }

    @Test
    @Tag("slow")
    void startPositionDepth5() {
        assertEquals(4_865_609L, perft(new ArrayList<>(), 5));
    }
}