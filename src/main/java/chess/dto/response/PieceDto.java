package chess.dto.response;

import chess.engine.model.piece.Color;
import chess.engine.model.piece.Piece;
import chess.engine.model.piece.PieceType;

public record PieceDto(
        PieceType type,
        Color color) {
    public PieceDto(Piece piece) {
        this(piece.getType(), piece.getColor());
    }
}