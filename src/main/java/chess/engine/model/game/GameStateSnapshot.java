package chess.engine.model.game;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Piece;

import java.util.Objects;

public record GameStateSnapshot(
        Board board,                 // Bản sao độc lập của bàn cờ
        Move lastMove,               // Nước đi vừa dẫn tới thế cờ này (null ở thế cờ đầu)
        Piece capturedPiece,         // Quân cờ bị ăn ở nước này (nếu có)
        Color turn,                  // Lượt đi tiếp theo
        CastleRights castleRights,   // Quyền nhập thành tại thời điểm này
        Position enPassTarget,       // Ô En Passant khả dụng
        int halfMoveClock,           // Bộ đếm 50 nước
        int fullMoveNumber,          // Số thứ tự nước đi
        boolean inCheck,             // Bên tới lượt có đang bị chiếu không
        GameResult gameResult        // Kết quả ván cờ tại thời điểm này
) {
    public GameStateSnapshot {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(turn, "Turn must not be null");
        Objects.requireNonNull(castleRights, "Castle rights must not be null");
        Objects.requireNonNull(gameResult, "Game result must not be null");

        if (halfMoveClock < 0 || fullMoveNumber <= 0) {
            throw new IllegalArgumentException("Half move clock must be non-negative and full move number must be positive");
        }
    }

    public String stateKey() {
        return board.getPieces().toString() + "|" + turn + "|" + castleRights + "|" + enPassTarget;
    }
}