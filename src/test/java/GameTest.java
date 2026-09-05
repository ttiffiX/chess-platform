import model.Game;
import model.Move;
import model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import model.piece.Color;
import model.piece.Queen;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Nested
    @DisplayName("Castling (Nhập thành)")
    class CastlingTests {

        @Test
        @DisplayName("Nên thực hiện nhập thành cánh Vua thành công và dịch chuyển cả Vua lẫn Xe")
        void shouldCastleKingSideSuccessfully() {
            Game game = new Game();

            // Mở đường cho Trắng: dọn Tốt e, Tốt g, Tượng f, Mã f
            game.move(new Move(new Position('e', 2), new Position('e', 4))); // W
            game.move(new Move(new Position('a', 7), new Position('a', 6))); // B
            game.move(new Move(new Position('g', 1), new Position('f', 3))); // W (Mã nhảy)
            game.move(new Move(new Position('a', 6), new Position('a', 5))); // B
            game.move(new Move(new Position('f', 1), new Position('e', 2))); // W (Tượng phát triển)
            game.move(new Move(new Position('b', 7), new Position('b', 6))); // B

            // Vua Trắng nhập thành: e1 -> g1
            game.move(new Move(new Position('e', 1), new Position('g', 1)));

            // Kiểm tra vị trí mới của Vua (g1) và Xe (f1)
            assertNull(game.getBoard().getPiece(new Position('e', 1)));
            assertNull(game.getBoard().getPiece(new Position('h', 1)));
            assertNotNull(game.getBoard().getPiece(new Position('g', 1)));
            assertNotNull(game.getBoard().getPiece(new Position('f', 1)));

            // Quyền nhập thành của Trắng phải mất sạch
            assertFalse(game.getCastleRights().whiteKingSide());
            assertFalse(game.getCastleRights().whiteQueenSide());
        }

        @Test
        @DisplayName("Không được nhập thành nếu đường đi giữa Vua và Xe có quân cản")
        void shouldRejectCastlingWhenPathIsBlocked() {
            Game game = new Game();

            // Chỉ đi tốt e4 và Tượng e2, Mã g1 vẫn chưa đi
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('e', 7), new Position('e', 5)));
            game.move(new Move(new Position('f', 1), new Position('e', 2)));
            game.move(new Move(new Position('d', 7), new Position('d', 5)));

            // Thử nhập thành khi g1 vẫn còn Mã -> Phải ném ngoại lệ
            assertThrows(IllegalArgumentException.class, () ->
                    game.move(new Move(new Position('e', 1), new Position('g', 1)))
            );
        }
    }

    @Nested
    @DisplayName("En Passant (Bắt tốt qua đường)")
    class EnPassantTests {

        @Test
        @DisplayName("Nên thực hiện En Passant hợp lệ và loại bỏ tốt đối phương ở hàng ngang")
        void shouldExecuteEnPassantSuccessfully() {
            Game game = new Game();

            // 1. e4 a6  2. e5 d5 (Tốt Đen d7 nhảy 2 bước lên d5)
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('e', 4), new Position('e', 5)));
            game.move(new Move(new Position('d', 7), new Position('d', 5))); // Đen kích hoạt En Passant target d6

            // 3. Tốt Trắng e5 ăn chéo sang d6 (En Passant)
            game.move(new Move(new Position('e', 5), new Position('d', 6)));

            // Tốt Trắng phải nằm ở d6
            assertNotNull(game.getBoard().getPiece(new Position('d', 6)));
            assertEquals(Color.WHITE, game.getBoard().getPiece(new Position('d', 6)).getColor());

            // Tốt Đen ở d5 phải bị XÓA KHỎI BÀN CỜ
            assertNull(game.getBoard().getPiece(new Position('d', 5)));
            assertNull(game.getBoard().getPiece(new Position('e', 5)));
        }

        @Test
        @DisplayName("Mất quyền En Passant nếu không ăn ngay ở lượt kế tiếp")
        void shouldExpireEnPassantIfIgnoredForOneTurn() {
            Game game = new Game();

            // Tạo thế cờ: Tốt Trắng e5, Tốt Đen vừa nhảy d7->d5
            game.move(new Move(new Position('e', 2), new Position('e', 4)));
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('e', 4), new Position('e', 5)));
            game.move(new Move(new Position('d', 7), new Position('d', 5)));

            // Trắng bỏ qua En Passant, đi Mã lên f3
            game.move(new Move(new Position('g', 1), new Position('f', 3)));
            // Đen đi một nước vu vơ
            game.move(new Move(new Position('h', 7), new Position('h', 6)));

            // Giờ Trắng thử ăn e5 -> d6 (đã hết quyền) -> Phải bị từ chối
            assertThrows(IllegalArgumentException.class, () ->
                    game.move(new Move(new Position('e', 5), new Position('d', 6)))
            );
        }
    }

    @Nested
    @DisplayName("Pawn Promotion (Phong cấp)")
    class PromotionTests {

        @Test
        @DisplayName("Phong cấp thành công: Tốt chạm hàng 8 biến thành Hậu")
        void shouldPromotePawnToQueenSuccessfully() {
            Game game = new Game();

            // Mở đường cho tốt Trắng tiến thẳng lên phong cấp (Dàn xếp nước đi)
            // Trắng dọn đường cột a và b
            game.move(new Move(new Position('a', 2), new Position('a', 4)));
            game.move(new Move(new Position('b', 7), new Position('b', 5)));
            game.move(new Move(new Position('a', 4), new Position('b', 5))); // Ăn tốt b5
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('b', 5), new Position('b', 6)));
            game.move(new Move(new Position('a', 6), new Position('a', 5)));
            game.move(new Move(new Position('b', 6), new Position('b', 7)));
            game.move(new Move(new Position('a', 5), new Position('a', 4)));

            // Nước đi quyết định: b7 -> b8 và phong cấp thành HẬU (Queen)
            Move promotionMove = new Move(
                    new Position('b', 7),
                    new Position('a', 8),
                    new Queen(Color.WHITE)
            );
            game.move(promotionMove);

            // Kiểm tra: ô b7 trống, ô b8 là Hậu Trắng
            assertNull(game.getBoard().getPiece(new Position('b', 7)));
            var pieceAtB8 = game.getBoard().getPiece(new Position('a', 8));
            assertNotNull(pieceAtB8);
            assertTrue(pieceAtB8 instanceof Queen);
            assertEquals(Color.WHITE, pieceAtB8.getColor());
        }

        @Test
        @DisplayName("Từ chối nếu Tốt chạm hàng 8 mà không truyền quân phong cấp")
        void shouldRejectPromotionWithoutPromotedPiece() {
            Game game = new Game();

            // Đưa tốt lên b7 giống như trên
            game.move(new Move(new Position('a', 2), new Position('a', 4)));
            game.move(new Move(new Position('b', 7), new Position('b', 5)));
            game.move(new Move(new Position('a', 4), new Position('b', 5)));
            game.move(new Move(new Position('a', 7), new Position('a', 6)));
            game.move(new Move(new Position('b', 5), new Position('b', 6)));
            game.move(new Move(new Position('a', 6), new Position('a', 5)));
            game.move(new Move(new Position('b', 6), new Position('b', 7)));
            game.move(new Move(new Position('a', 5), new Position('a', 4)));

            // Đi b7 -> b8 nhưng không chỉ định quân muốn đổi (truyền null)
            assertThrows(IllegalArgumentException.class, () ->
                    game.move(new Move(new Position('b', 7), new Position('b', 8)))
            );
        }
    }
}