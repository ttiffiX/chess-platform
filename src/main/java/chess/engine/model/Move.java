package chess.engine.model;

import chess.engine.model.piece.Piece;

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
}
