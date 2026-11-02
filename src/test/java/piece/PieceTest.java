package piece;

import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Piece Tests")
class PieceTest {

    @Test
    @DisplayName("Should create all piece types with WHITE color")
    void shouldCreateAllWhitePieces() {
        // Mỗi quân cờ khi được khởi tạo với WHITE phải giữ đúng màu.
        // Đây là thuộc tính chung được kế thừa từ Piece.

        List<Piece> pieces = List.of(
                new King(Color.WHITE),
                new Queen(Color.WHITE),
                new Rook(Color.WHITE),
                new Bishop(Color.WHITE),
                new Knight(Color.WHITE),
                new Pawn(Color.WHITE)
        );

        // Kiểm tra tất cả quân, tránh trường hợp một constructor
        // vô tình gán sai màu mà các quân khác vẫn được kiểm tra bình thường.
        assertAll(
                pieces.stream()
                        .map(piece -> (org.junit.jupiter.api.function.Executable)
                                () -> assertEquals(Color.WHITE, piece.getColor(),
                                        piece.getType() + " should have WHITE color")
                        )
        );
    }

    @Test
    @DisplayName("Should create all piece types with BLACK color")
    void shouldCreateAllBlackPieces() {
        // Tương tự WHITE, các quân phải giữ đúng màu BLACK khi khởi tạo.

        List<Piece> pieces = List.of(
                new King(Color.BLACK),
                new Queen(Color.BLACK),
                new Rook(Color.BLACK),
                new Bishop(Color.BLACK),
                new Knight(Color.BLACK),
                new Pawn(Color.BLACK)
        );

        assertAll(
                pieces.stream()
                        .map(piece -> (org.junit.jupiter.api.function.Executable)
                                () -> assertEquals(Color.BLACK, piece.getColor(),
                                        piece.getType() + " should have BLACK color")
                        )
        );
    }

    @Test
    @DisplayName("Should reject null color for all piece types")
    void shouldRejectNullColor() {
        // Mọi quân cờ bắt buộc phải có màu WHITE hoặc BLACK.
        // Không chấp nhận một quân cờ có màu null.

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new King(null)),
                () -> assertThrows(NullPointerException.class, () -> new Queen(null)),
                () -> assertThrows(NullPointerException.class, () -> new Rook(null)),
                () -> assertThrows(NullPointerException.class, () -> new Bishop(null)),
                () -> assertThrows(NullPointerException.class, () -> new Knight(null)),
                () -> assertThrows(NullPointerException.class, () -> new Pawn(null))
        );
    }
}