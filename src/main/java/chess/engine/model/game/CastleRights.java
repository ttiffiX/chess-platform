package chess.engine.model.game;

import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.King;
import chess.engine.model.piece.Piece;
import chess.engine.model.piece.Rook;

public record CastleRights(boolean whiteKingSide, boolean whiteQueenSide, boolean blackKingSide,
                           boolean blackQueenSide) {
    public static CastleRights initial() {
        return new CastleRights(true, true, true, true);
    }

    public boolean canCastleKingSide(Color color) {
        validateColor(color);
        return color == Color.WHITE ? whiteKingSide : blackKingSide;
    }

    public boolean canCastleQueenSide(Color color) {
        validateColor(color);
        return color == Color.WHITE ? whiteQueenSide : blackQueenSide;
    }

    public CastleRights withMoveApplied(Piece movedPiece, Position from, Piece capturedPiece, Position to) {
        if (movedPiece == null || from == null || to == null) {
            throw new IllegalArgumentException("Moved piece, from and to must not be null.");
        }

        CastleRights updatedRights = this;

        if (movedPiece instanceof King) {
            updatedRights = updatedRights.withoutCastling(movedPiece.getColor());
        } else if (movedPiece instanceof Rook) {
            updatedRights = updatedRights.withoutCastlingRightForRookSquare(movedPiece.getColor(), from);
        }

        if (capturedPiece instanceof Rook) {
            updatedRights = updatedRights.withoutCastlingRightForRookSquare(capturedPiece.getColor(), to);
        }

        return updatedRights;
    }

    private CastleRights withoutCastlingRightForRookSquare(Color color, Position rookPosition) {
        if (color == Color.WHITE) {
            if (rookPosition.equals(new Position('a', 1))) {
                return withoutQueenSide(Color.WHITE);
            }
            if (rookPosition.equals(new Position('h', 1))) {
                return withoutKingSide(Color.WHITE);
            }
            return this;
        }

        if (rookPosition.equals(new Position('a', 8))) {
            return withoutQueenSide(Color.BLACK);
        }
        if (rookPosition.equals(new Position('h', 8))) {
            return withoutKingSide(Color.BLACK);
        }
        return this;
    }

    private CastleRights withoutKingSide(Color color) {
        validateColor(color);
        return color == Color.WHITE
                ? new CastleRights(false, whiteQueenSide, blackKingSide, blackQueenSide)
                : new CastleRights(whiteKingSide, whiteQueenSide, false, blackQueenSide);
    }

    private CastleRights withoutQueenSide(Color color) {
        validateColor(color);
        return color == Color.WHITE
                ? new CastleRights(whiteKingSide, false, blackKingSide, blackQueenSide)
                : new CastleRights(whiteKingSide, whiteQueenSide, blackKingSide, false);
    }

    private CastleRights withoutCastling(Color color) {
        return withoutKingSide(color).withoutQueenSide(color);
    }

    private static void validateColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color must not be null.");
        }
    }
}
