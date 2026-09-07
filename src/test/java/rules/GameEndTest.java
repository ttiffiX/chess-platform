package rules;

import model.Move;
import model.Position;
import model.game.Game;
import model.game.GameStatus;
import model.piece.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEndTest {

    @Nested
    @DisplayName("Thế chiếu (Check)")
    class CheckTests {

        @Test
        @DisplayName("Cờ inCheck phải bật true khi Vua bị đe dọa")
        void shouldDetectCheck() {
            Game game = new Game();

            // 1. e4 e5  2. Qh5 Nc6  3. Qxf7+
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('d', 1), new Position('h', 5)));
            game.move(new Move(new Position('b', 8), new Position('c', 6)));
            game.move(new Move(new Position('h', 5), new Position('f', 7)));

            assertTrue(game.isInCheck());
            assertEquals(Color.BLACK, game.getCurrentTurn());
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
        }
    }

    @Nested
    @DisplayName("Chiếu bí (Checkmate)")
    class CheckmateTests {

        @Test
        @DisplayName("Fool's Mate (Chiếu bí nhanh nhất trong 2 nước) -> Đen thắng")
        void shouldDetectFoolsMateBlackWins() {
            Game game = new Game();

            // 1. f3 e5  2. g4 Qh4#
            game.move(new Move(new Position('f', 2), new Position('f', 3)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('g', 2), new Position('g', 4)));
            game.move(new Move(new Position('d', 8), new Position('h', 4)));

            assertTrue(game.isInCheck());
            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());
            assertEquals(Color.BLACK, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Scholar's Mate (Chiếu bí 4 nước) -> Trắng thắng")
        void shouldDetectScholarsMateWhiteWins() {
            Game game = new Game();

            // 1. e4 e5  2. Bc4 Nc6  3. Qh5 Nf6  4. Qxf7#
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('f', 1), new Position('c', 4)));
            game.move(new Move(new Position('b', 8), new Position('c', 6)));
            game.move(new Move(new Position('d', 1), new Position('h', 5)));
            game.move(new Move(new Position('g', 8), new Position('f', 6)));
            game.move(new Move(new Position('h', 5), new Position('f', 7)));

            assertTrue(game.isInCheck());
            assertEquals(Color.BLACK, game.getCurrentTurn());
            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());
            assertEquals(Color.WHITE, game.getGameResult().winner());
        }
    }

    @Nested
    @DisplayName("Các luật hòa cờ (Draws)")
    class DrawTests {

        @Test
        @DisplayName("Hòa do lặp lại thế cờ 3 lần (Threefold Repetition)")
        void shouldDetectDrawByThreefoldRepetition() {
            Game game = new Game();

            // Lặp lại nước đi của 2 Mã để đưa bàn cờ về cùng thế 3 lần
            // Lần 1: Thế cờ khởi đầu (đã lưu sẵn trong snapshot ban đầu)
            game.move(new Move(new Position('g', 1), new Position('f', 3)));
            game.move(new Move(new Position('g', 8), new Position('f', 6)));
            game.move(new Move(new Position('f', 3), new Position('g', 1)));
            game.move(new Move(new Position('f', 6), new Position('g', 8))); // Lần 2

            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());

            // Lặp thêm 1 vòng nữa
            game.move(new Move(new Position('g', 1), new Position('f', 3)));
            game.move(new Move(new Position('g', 8), new Position('f', 6)));
            game.move(new Move(new Position('f', 3), new Position('g', 1)));
            game.move(new Move(new Position('f', 6), new Position('g', 8))); // Lần 3 đạt điều kiện lặp lại

            assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.getGameResult().status());
            assertNull(game.getGameResult().winner());
        }

        @Test
        @DisplayName("fullMoveNumber tăng sau khi Đen đi, halfMoveClock tăng khi đi Mã")
        void shouldUpdateMoveCountersProperly() {
            Game game = new Game();

            assertEquals(1, game.getFullMoveNumber());

            // Trắng đi Tốt -> halfMoveClock = 0
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            assertEquals(1, game.getFullMoveNumber());

            // Đen đi Tốt -> fullMoveNumber = 2, halfMoveClock = 0
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            assertEquals(2, game.getFullMoveNumber());

            // Trắng đi Mã -> không phải tốt, không ăn quân -> halfMoveClock = 1
            game.move(new Move(new Position('g', 1), new Position('f', 3)));
            // Đen đi Mã -> fullMoveNumber = 3, halfMoveClock = 2
            game.move(new Move(new Position('b', 8), new Position('c', 6)));
            assertEquals(3, game.getFullMoveNumber());
        }
    }
}