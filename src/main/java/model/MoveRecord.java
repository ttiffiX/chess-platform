package model;

import piece.Piece;

public record MoveRecord(Piece piece, Move move, Piece capturedPiece) {
    public MoveRecord {
        if (piece == null || move == null) {
            throw new IllegalArgumentException("Piece and move cannot be null");
        }
    }

    public MoveRecord(Piece piece, Move move) {
        this(piece, move, null);
    }
}
