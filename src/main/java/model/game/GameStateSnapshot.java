package model.game;

import model.Board;
import model.Move;
import model.Position;
import model.piece.Color;
import model.piece.Piece;

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
        if (board == null || turn == null || castleRights == null || gameResult == null) {
            throw new IllegalArgumentException("Essential snapshot fields cannot be null");
        }

        if (halfMoveClock < 0 || fullMoveNumber <= 0) {
            throw new IllegalArgumentException("Half move clock must be non-negative and full move number must be positive");
        }
    }

    public String stateKey() {
        return board.getPieces().toString() + "|" + turn + "|" + castleRights + "|" + enPassTarget;
    }
}