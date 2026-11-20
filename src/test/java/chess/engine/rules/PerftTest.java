
package chess.engine.rules;

import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.utils.FenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Perft Tests")
class PerftTest {

    private static final String KIWIPETE =
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    private static final String POSITION_3 =
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";

    private static final String POSITION_4 =
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";

    private static final String POSITION_5 =
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";

    private static final String POSITION_6 =
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";

    private static long perft(Game game, int depth) {
        if (depth == 0) {
            return 1;
        }

        List<Move> moves = List.copyOf(game.getAllLegalMovesByColor());

        if (depth == 1) {
            return moves.size();
        }

        long nodes = 0;
        String parentFen = game.getCurrentFEN();

        for (Move move : moves) {
            Game child = new Game(FenUtils.parseFen(parentFen));

            var result = child.move(move);

            assertTrue(
                    result.valid(),
                    () -> "Invalid move: " + move.toUCI()
                            + ", reason: " + result.reason()
                            + ", parent FEN: " + parentFen
            );

            nodes += perft(child, depth - 1);
        }

        return nodes;
    }

    private static long perft(int depth) {
        return perft(new Game(), depth);
    }

    private static long perft(String fen, int depth) {
        return perft(new Game(FenUtils.parseFen(fen)), depth);
    }

    // ---------------------------------------------------------
    // Initial position
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 20",
            "2, 400",
            "3, 8902",
            "4, 197281"
    })
    @DisplayName("Should match initial position perft counts")
    void startPosition(int depth, long expected) {
        assertEquals(expected, perft(depth));
    }

    @Test
    @Tag("slow")
    @DisplayName("Should match initial position depth 5")
    void startPositionDepth5() {
        assertEquals(4_865_609L, perft(5));
    }

    // ---------------------------------------------------------
    // Kiwipete - castling and complex legal move generation
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 48",
            "2, 2039",
            "3, 97862"
    })
    @DisplayName("Should match Kiwipete perft counts")
    void kiwipete(int depth, long expected) {
        assertEquals(expected, perft(KIWIPETE, depth));
    }

    // ---------------------------------------------------------
    // Position 3 - endgame and en passant
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 14",
            "2, 191",
            "3, 2812",
            "4, 43238"
    })
    @DisplayName("Should match position 3 perft counts")
    void position3(int depth, long expected) {
        assertEquals(expected, perft(POSITION_3, depth));
    }

    // ---------------------------------------------------------
    // Position 4 - promotions and checks
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 6",
            "2, 264",
            "3, 9467"
    })
    @DisplayName("Should match position 4 perft counts")
    void position4(int depth, long expected) {
        assertEquals(expected, perft(POSITION_4, depth));
    }

    // ---------------------------------------------------------
    // Position 5 - promotion-heavy position
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 44",
            "2, 1486",
            "3, 62379"
    })
    @DisplayName("Should match position 5 perft counts")
    void position5(int depth, long expected) {
        assertEquals(expected, perft(POSITION_5, depth));
    }

    // ---------------------------------------------------------
    // Position 6 - complex tactical position
    // ---------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "1, 46",
            "2, 2079",
            "3, 89890"
    })
    @DisplayName("Should match position 6 perft counts")
    void position6(int depth, long expected) {
        assertEquals(expected, perft(POSITION_6, depth));
    }
}