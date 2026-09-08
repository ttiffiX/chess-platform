package model;

import chess.chess_engine.model.Move;
import chess.chess_engine.model.Position;
import chess.chess_engine.model.game.Game;
import chess.chess_engine.model.game.GameStateSnapshot;
import chess.chess_engine.model.game.GameStatus;
import chess.chess_engine.model.piece.Color;
import chess.chess_engine.model.piece.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    @DisplayName("Mô phỏng ván cờ Opera Game kinh điển (Paul Morphy)")
    void testOperaGameSimulation() {
        Game game = new Game();

        // 1. e4 e5
        game.move(new Move(new Position('e', 2), new Position('e', 4)));
        game.move(new Move(new Position('e', 7), new Position('e', 5)));

        // 2. Nf3 d6
        game.move(new Move(new Position('g', 1), new Position('f', 3)));
        game.move(new Move(new Position('d', 7), new Position('d', 6)));

        // 3. d4 Bg4
        game.move(new Move(new Position('d', 2), new Position('d', 4)));
        game.move(new Move(new Position('c', 8), new Position('g', 4)));

        // 4. dxe5 Bxf3
        game.move(new Move(new Position('d', 4), new Position('e', 5)));
        game.move(new Move(new Position('g', 4), new Position('f', 3)));

        // 5. Qxf3 dxe5
        game.move(new Move(new Position('d', 1), new Position('f', 3)));
        game.move(new Move(new Position('d', 6), new Position('e', 5)));

        // 6. Bc4 Nf6
        game.move(new Move(new Position('f', 1), new Position('c', 4)));
        game.move(new Move(new Position('g', 8), new Position('f', 6)));

        // 7. Qb3 Qe7
        game.move(new Move(new Position('f', 3), new Position('b', 3)));
        game.move(new Move(new Position('d', 8), new Position('e', 7)));

        // Kiểm tra bàn cờ vẫn đang tiếp diễn mượt mà, không ngoại lệ
        assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
        assertFalse(game.isInCheck());
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Nested
    @DisplayName("Vòng đời ván đấu cơ bản (Turn, Guard & Simulation)")
    class LifecycleTests {

        @Test
        @DisplayName("Khởi tạo ván cờ với các giá trị mặc định chính xác")
        void shouldInitializeCorrectly() {
            Game game = new Game();

            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertFalse(game.isInCheck());
            assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
            assertEquals(1, game.getFullMoveNumber());
            assertEquals(1, game.getGameStateHistory().size()); // 1 snapshot ban đầu
            assertTrue(game.getCapturedPieces().isEmpty());
        }

        @Test
        @DisplayName("Đổi lượt chính xác giữa Trắng và Đen sau mỗi nước đi hợp lệ")
        void shouldAlternateTurns() {
            Game game = new Game();

            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            assertEquals(Color.BLACK, game.getCurrentTurn());

            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            assertEquals(Color.WHITE, game.getCurrentTurn());
        }

        @Test
        @DisplayName("Ném lỗi nếu đi sai lượt hoặc thực hiện nước đi bất hợp lệ")
        void shouldRejectInvalidTurnAndIllegalMove() {
            Game game = new Game();

            // Lượt Trắng nhưng cố đi quân Đen
            assertThrows(IllegalArgumentException.class, () ->
                    game.move(new Move(new Position('e', 7), new Position('e', 5)))
            );

            // Nước đi sai hình học
            assertThrows(IllegalArgumentException.class, () ->
                    game.move(new Move(new Position('e', 2), new Position('e', 5)))
            );
        }

        @Test
        @DisplayName("Chặn tuyệt đối mọi nước đi tiếp theo khi game đã kết thúc")
        void shouldRejectMoveWhenGameIsOver() {
            Game game = new Game();

            // Fool's Mate dẫn tới kết thúc ván
            game.move(new Move(new Position('f', 2), new Position('f', 3)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('g', 2), new Position('g', 4)));
            game.move(new Move(new Position('d', 8), new Position('h', 4)));

            assertNotEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());

            // Nước đi sau khi kết thúc ván cờ phải ném IllegalStateException
            assertThrows(IllegalStateException.class, () ->
                    game.move(new Move(new Position('a', 2), new Position('a', 3)))
            );
        }

        @Test
        @DisplayName("Mô phỏng chuỗi nước đi Opera Game và truy xuất snapshot xem lại (Review)")
        void shouldSimulateMovesAndAllowSnapshotReview() {
            Game game = new Game();

            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('g', 1), new Position('f', 3)));

            // Tổng snapshot: 1 bản đầu + 3 nước đi = 4
            assertEquals(4, game.getGameStateHistory().size());

            // Review lại snapshot tại nước thứ 1 (sau nước e4 của Trắng)
            GameStateSnapshot snapshot1 = game.getSnapshotAt(1);
            assertEquals(Color.BLACK, snapshot1.turn());
            assertNotNull(snapshot1.board().getPiece(new Position('e', 4)));
        }
    }

    @Nested
    @DisplayName("Thống kê danh sách quân bị ăn (Captured Pieces)")
    class CapturedPiecesTests {

        @Test
        @DisplayName("Ghi nhận đúng quân bị ăn và phân loại theo màu quân")
        void shouldTrackCapturedPiecesByColor() {
            Game game = new Game();

            // 1. e4 d5  2. exd5 Qxd5
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('d', 7), new Position('d', 5)));
            game.move(new Move(new Position('e', 4), new Position('d', 5))); // Trắng ăn Tốt Đen
            game.move(new Move(new Position('d', 8), new Position('d', 5))); // Đen ăn Tốt Trắng

            List<Piece> blackCaptured = game.getCapturedPiecesByColor(Color.BLACK);
            List<Piece> whiteCaptured = game.getCapturedPiecesByColor(Color.WHITE);

            assertEquals(1, blackCaptured.size());
            assertEquals(Color.BLACK, blackCaptured.getFirst().getColor());

            assertEquals(1, whiteCaptured.size());
            assertEquals(Color.WHITE, whiteCaptured.getFirst().getColor());
            assertEquals(2, game.getCapturedPieces().size());
        }
    }

    @Nested
    @DisplayName("Tính năng Hoàn tác (Undo)")
    class UndoTests {

        @Test
        @DisplayName("Undo đưa bàn cờ và lượt đi về nước đi liền trước")
        void shouldUndoStandardMove() {
            Game game = new Game();

            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            assertEquals(Color.BLACK, game.getCurrentTurn());
            assertNotNull(game.getBoard().getPiece(new Position('e', 4)));

            boolean undone = game.undo();
            assertTrue(undone);
            assertEquals(Color.WHITE, game.getCurrentTurn());
            assertNull(game.getBoard().getPiece(new Position('e', 4)));
            assertNotNull(game.getBoard().getPiece(new Position('e', 2)));
            assertEquals(1, game.getGameStateHistory().size());

            // Không thể undo khi đã về trạng thái ban đầu của bàn cờ
            assertFalse(game.undo());
        }

        @Test
        @DisplayName("Undo nước ăn quân phải trả lại quân cờ bị ăn và xóa khỏi danh sách captured")
        void shouldUndoCaptureMoveProperly() {
            Game game = new Game();

            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('d', 7), new Position('d', 5)));
            game.move(new Move(new Position('e', 4), new Position('d', 5))); // Ăn quân

            assertEquals(1, game.getCapturedPieces().size());

            assertTrue(game.undo());

            // Tốt Đen phải hồi sinh lại ở d5, Tốt Trắng lùi về e4
            assertNotNull(game.getBoard().getPiece(new Position('e', 4)));
            assertNotNull(game.getBoard().getPiece(new Position('d', 5)));
            assertEquals(Color.BLACK, game.getBoard().getPiece(new Position('d', 5)).getColor());
            assertTrue(game.getCapturedPieces().isEmpty());
            assertEquals(Color.WHITE, game.getCurrentTurn());
        }

        @Test
        @DisplayName("Undo nước Nhập thành đưa cả Vua và Xe về ô gốc và khôi phục quyền nhập thành")
        void shouldUndoCastling() {
            Game game = new Game();

            // Mở đường nhập thành cánh Vua cho Trắng
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('g', 1), new Position('f', 3)));
            game.move(new Move(new Position('a', 6), new Position('a', 5)));
            game.move(new Move(new Position('f', 1), new Position('e', 2)));
            game.move(new Move(new Position('b', 7), new Position('b', 6)));

            // Nhập thành e1 -> g1 (Xe h1 -> f1)
            game.move(new Move(new Position('e', 1), new Position('g', 1)));
            assertFalse(game.getCastleRights().canCastleKingSide(Color.WHITE));

            assertTrue(game.undo());

            // Khôi phục vị trí và quyền nhập thành
            assertNotNull(game.getBoard().getPiece(new Position('e', 1)));
            assertNotNull(game.getBoard().getPiece(new Position('h', 1)));
            assertNull(game.getBoard().getPiece(new Position('g', 1)));
            assertNull(game.getBoard().getPiece(new Position('f', 1)));
            assertTrue(game.getCastleRights().canCastleKingSide(Color.WHITE));
        }

        @Test
        @DisplayName("Undo nước En Passant khôi phục lại Tốt đối phương ở hàng ngang")
        void shouldUndoEnPassant() {
            Game game = new Game();

            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('e', 4), new Position('e', 5)));
            game.move(new Move(new Position('d', 7), new Position('d', 5)));

            // Ăn En Passant
            game.move(new Move(new Position('e', 5), new Position('d', 6)));
            assertNull(game.getBoard().getPiece(new Position('d', 5)));

            assertTrue(game.undo());

            // Tốt Đen phải phục hồi lại tại d5
            assertNotNull(game.getBoard().getPiece(new Position('e', 5)));
            assertNotNull(game.getBoard().getPiece(new Position('d', 5)));
            assertNull(game.getBoard().getPiece(new Position('d', 6)));
            assertEquals(Color.WHITE, game.getCurrentTurn());
        }
    }
}