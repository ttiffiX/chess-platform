
package rules;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEndTest {

    private Move move(char fromFile, int fromRank, char toFile, int toRank) {
        return new Move(
                new Position(fromFile, fromRank),
                new Position(toFile, toRank)
        );
    }

    private void play(Game game, Move... moves) {
        for (Move move : moves) {
            game.move(move);
        }
    }

    @Nested
    @DisplayName("Thế chiếu (Check)")
    class CheckTests {

        @Test
        @DisplayName("Phát hiện chiếu khi Vua bị đe dọa")
        void shouldDetectCheck() {
            Game game = new Game();

            // 1. e4 e5 2. Qh5 Nc6 3. Qxf7+
            play(game,
                    move('e', 2, 'e', 4),
                    move('e', 7, 'e', 5),
                    move('d', 1, 'h', 5),
                    move('b', 8, 'c', 6),
                    move('h', 5, 'f', 7)
            );

            assertTrue(game.isInCheck());
            assertEquals(Color.BLACK, game.getCurrentTurn());
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
        }

        @Test
        @DisplayName("Không bị chiếu khi không có quân đối phương tấn công Vua")
        void shouldNotBeInCheckInitially() {
            Game game = new Game();

            assertFalse(game.isInCheck());
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
        }
    }

    @Nested
    @DisplayName("Chiếu bí (Checkmate)")
    class CheckmateTests {

        @Test
        @DisplayName("Fool's Mate - Đen thắng")
        void shouldDetectFoolsMateBlackWins() {
            Game game = new Game();

            // 1. f3 e5 2. g4 Qh4#
            play(game,
                    move('f', 2, 'f', 3),
                    move('e', 7, 'e', 5),
                    move('g', 2, 'g', 4),
                    move('d', 8, 'h', 4)
            );

            assertTrue(game.isInCheck());
            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());
            assertEquals(Color.BLACK, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Scholar's Mate - Trắng thắng")
        void shouldDetectScholarsMateWhiteWins() {
            Game game = new Game();

            // 1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6 4. Qxf7#
            play(game,
                    move('e', 2, 'e', 4),
                    move('e', 7, 'e', 5),
                    move('f', 1, 'c', 4),
                    move('b', 8, 'c', 6),
                    move('d', 1, 'h', 5),
                    move('g', 8, 'f', 6),
                    move('h', 5, 'f', 7)
            );

            assertTrue(game.isInCheck());
            assertEquals(Color.BLACK, game.getCurrentTurn());
            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());
            assertEquals(Color.WHITE, game.getGameResult().winner());
        }

        @Test
        @DisplayName("Không cho phép tiếp tục đi quân sau khi chiếu bí")
        void shouldNotAllowMovesAfterCheckmate() {
            Game game = new Game();

            // Fool's Mate
            play(game,
                    move('f', 2, 'f', 3),
                    move('e', 7, 'e', 5),
                    move('g', 2, 'g', 4),
                    move('d', 8, 'h', 4)
            );

            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());

            var resultBefore = game.getGameResult();
            var turnBefore = game.getCurrentTurn();

            // Trắng thử đi một nước sau khi đã bị chiếu bí
            game.move(move('e', 2, 'e', 4));

            assertEquals(resultBefore, game.getGameResult());
            assertEquals(turnBefore, game.getCurrentTurn());
            assertEquals(GameStatus.CHECKMATE, game.getGameResult().status());
        }
    }

    @Nested
    @DisplayName("Hòa cờ (Draw)")
    class DrawTests {

        @Test
        @DisplayName("Hòa khi một thế cờ được lặp lại 3 lần")
        void shouldDetectDrawByThreefoldRepetition() {
            Game game = new Game();

            // Thế cờ ban đầu: lần xuất hiện thứ nhất
            // Sau mỗi vòng, hai Mã trở về vị trí ban đầu.
            play(game,
                    move('g', 1, 'f', 3),
                    move('g', 8, 'f', 6),
                    move('f', 3, 'g', 1),
                    move('f', 6, 'g', 8)
            );

            // Lần xuất hiện thứ hai
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());

            play(game,
                    move('g', 1, 'f', 3),
                    move('g', 8, 'f', 6),
                    move('f', 3, 'g', 1),
                    move('f', 6, 'g', 8)
            );

            // Lần xuất hiện thứ ba
            assertEquals(
                    GameStatus.DRAW_THREEFOLD_REPETITION,
                    game.getGameResult().status()
            );
            assertNull(game.getGameResult().winner());
        }

        @Test
        @DisplayName("Không được tiếp tục đi quân sau khi hòa")
        void shouldNotAllowMovesAfterDraw() {
            Game game = new Game();

            // Lặp lại thế cờ 3 lần
            for (int i = 0; i < 2; i++) {
                play(game,
                        move('g', 1, 'f', 3),
                        move('g', 8, 'f', 6),
                        move('f', 3, 'g', 1),
                        move('f', 6, 'g', 8)
                );
            }

            assertEquals(
                    GameStatus.DRAW_THREEFOLD_REPETITION,
                    game.getGameResult().status()
            );

            var resultBefore = game.getGameResult();
            var turnBefore = game.getCurrentTurn();

            game.move(move('e', 2, 'e', 4));

            assertEquals(resultBefore, game.getGameResult());
            assertEquals(turnBefore, game.getCurrentTurn());
        }
    }

    @Nested
    @DisplayName("Bộ đếm nước đi")
    class MoveCounterTests {

        @Test
        @DisplayName("Khởi tạo bộ đếm đúng giá trị")
        void shouldInitializeCountersCorrectly() {
            Game game = new Game();

            assertEquals(1, game.getFullMoveNumber());
            assertEquals(0, game.getHalfMoveClock());
        }

        @Test
        @DisplayName("fullMoveNumber tăng sau mỗi nước đi của Đen")
        void shouldIncreaseFullMoveNumberAfterBlackMove() {
            Game game = new Game();

            game.move(move('e', 2, 'e', 4));
            assertEquals(1, game.getFullMoveNumber());

            game.move(move('e', 7, 'e', 5));
            assertEquals(2, game.getFullMoveNumber());

            game.move(move('g', 1, 'f', 3));
            assertEquals(2, game.getFullMoveNumber());

            game.move(move('b', 8, 'c', 6));
            assertEquals(3, game.getFullMoveNumber());
        }

        @Test
        @DisplayName("halfMoveClock tăng khi đi quân không phải Tốt và không ăn quân")
        void shouldIncreaseHalfMoveClockForNonPawnMove() {
            Game game = new Game();

            // Tốt đi: reset về 0
            game.move(move('e', 2, 'e', 4));
            assertEquals(0, game.getHalfMoveClock());

            game.move(move('e', 7, 'e', 5));
            assertEquals(0, game.getHalfMoveClock());

            // Mã đi: tăng lên 1
            game.move(move('g', 1, 'f', 3));
            assertEquals(1, game.getHalfMoveClock());

            // Mã Đen đi: tăng lên 2
            game.move(move('b', 8, 'c', 6));
            assertEquals(2, game.getHalfMoveClock());
        }

        @Test
        @DisplayName("halfMoveClock reset khi Tốt di chuyển")
        void shouldResetHalfMoveClockAfterPawnMove() {
            Game game = new Game();

            play(game,
                    move('g', 1, 'f', 3),
                    move('g', 8, 'f', 6)
            );

            assertEquals(2, game.getHalfMoveClock());

            game.move(move('e', 2, 'e', 4));

            assertEquals(0, game.getHalfMoveClock());
        }

        @Test
        @DisplayName("halfMoveClock reset khi có quân bị ăn")
        void shouldResetHalfMoveClockAfterCapture() {
            Game game = new Game();

            play(game,
                    move('g', 1, 'f', 3), // Trắng: Mã
                    move('g', 8, 'f', 6), // Đen: Mã
                    move('e', 2, 'e', 4), // Trắng: Tốt
                    move('d', 7, 'd', 5), // Đen: Tốt
                    move('f', 3, 'g', 1), // Trắng: Mã về
                    move('b', 8, 'c', 6)  // Đen: Mã
            );

            assertEquals(2, game.getHalfMoveClock());

            // Trắng ăn Tốt Đen tại d5
            game.move(move('e', 4, 'd', 5));

            assertEquals(0, game.getHalfMoveClock());
        }
    }
}
