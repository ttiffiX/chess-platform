package chess.chess_engine.model;

import chess.chess_engine.model.piece.Piece;

public record Move(Position from, Position to, Piece promotedPiece) {
    public Move {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid move: from and to positions cannot be null");
        }

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
}
