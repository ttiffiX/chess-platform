package chess.dto.request;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Piece;
import chess.engine.model.piece.PieceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MoveRequest(
        @NotBlank(message = "From position cannot be blank")
        @Pattern(regexp = "^[a-hA-H][1-8]$", message = "From position must be a valid chess position (e.g., a1, h8)")
        String from,

        @NotBlank(message = "To position cannot be blank")
        @Pattern(regexp = "^[a-hA-H][1-8]$", message = "To position must be a valid chess position (e.g., a1, h8)")
        String to,

        PieceType promotionType
) {
    public Move toMove(Color turn) {
        Piece piece = promotionType != null ? promotionType.getPiece(turn) : null;
        return new Move(parsePosition(from), parsePosition(to), piece);
    }

    private static Position parsePosition(String pos) {
        char file = Character.toLowerCase(pos.charAt(0));
        int rank = Character.getNumericValue(pos.charAt(1));
        return new Position(file, rank);
    }
}