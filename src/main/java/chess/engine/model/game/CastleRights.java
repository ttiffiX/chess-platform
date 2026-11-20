package chess.engine.model.game;

import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.King;
import chess.engine.model.piece.Piece;
import chess.engine.model.piece.Rook;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        Objects.requireNonNull(movedPiece, "Moved piece must not be null");
        Objects.requireNonNull(from, "From position must not be null");
        Objects.requireNonNull(to, "To position must not be null");

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

    public String createCastleRightsKey() {
        StringBuilder key = new StringBuilder();

        if (whiteKingSide()) key.append('K');
        if (whiteQueenSide()) key.append('Q');
        if (blackKingSide()) key.append('k');
        if (blackQueenSide()) key.append('q');

        return key.isEmpty() ? "-" : key.toString();
    }

    public static CastleRights createCastleRightsFromKey(String key) {
        if (key == null || key.isBlank() || key.equals("-")) {
            return new CastleRights(false, false, false, false);
        }

        Set<Character> seen = new HashSet<>();
        for (char ch : key.toCharArray()) {
            if (!"KQkq".contains(String.valueOf(ch))) {
                throw new IllegalArgumentException("Invalid castling right in key: " + key);
            }
            if (!seen.add(ch)) { // Set.add trả về false nếu phần tử đã tồn tại
                throw new IllegalArgumentException("Duplicate castling right in key: " + key);
            }
        }

        return new CastleRights(
                seen.contains('K'),
                seen.contains('Q'),
                seen.contains('k'),
                seen.contains('q')
        );
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
        Objects.requireNonNull(color, "Color must not be null");
    }
}
