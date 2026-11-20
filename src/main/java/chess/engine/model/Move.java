package chess.engine.model;

import chess.engine.model.piece.Color;
import chess.engine.model.piece.Piece;
import chess.engine.model.piece.PieceType;

import java.util.Objects;

public record Move(Position from, Position to, Piece promotedPiece) {
    public Move {
        Objects.requireNonNull(from, "From position must not be null");
        Objects.requireNonNull(to, "To position must not be null");

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move: from and to positions are the same");
        }
    }

    public Move(Position from, Position to) {
        this(from, to, null);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

    public String toUCI() {
        StringBuilder uci = new StringBuilder();
        uci.append(from.toString().toLowerCase());
        uci.append(to.toString().toLowerCase());
        if (promotedPiece != null) {
            uci.append(promotedPiece.getType().getUciChar());
        }
        return uci.toString();
    }

    public static Move fromUCI(String uci, Color currentTurn) {
        if (uci == null || !uci.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
            throw new IllegalArgumentException("Invalid UCI string: " + uci);
        }

        Position from = new Position(uci.charAt(0), uci.charAt(1) - '0');

        Position to = new Position(uci.charAt(2), uci.charAt(3) - '0');
        Piece promotedPiece = null;

        if (uci.length() == 5) {
            promotedPiece = PieceType.fromUciChar(String.valueOf(uci.charAt(4))).getPiece(currentTurn);
        }

        return new Move(from, to, promotedPiece);
    }
}
