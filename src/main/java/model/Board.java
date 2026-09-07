package model;

import model.piece.*;

import java.util.HashMap;
import java.util.Map;

public class Board {
    private final Map<Position, Piece> pieces = new HashMap<>();

    public Board() {
        initializeBoard();
    }

    public Board(Map<Position, Piece> pieces) {
        if (pieces == null) {
            throw new IllegalArgumentException("Pieces must not be null.");
        }
        this.pieces.putAll(pieces);
    }

    private void initializeBoard() {
        for (char file = 'a'; file <= 'h'; file++) {
            pieces.put(new Position(file, 2), new Pawn(Color.WHITE));
            pieces.put(new Position(file, 7), new Pawn(Color.BLACK));
        }

        for (char file : new char[]{'a', 'h'}) {
            pieces.put(new Position(file, 1), new Rook(Color.WHITE));
            pieces.put(new Position(file, 8), new Rook(Color.BLACK));
        }

        for (char file : new char[]{'b', 'g'}) {
            pieces.put(new Position(file, 1), new Knight(Color.WHITE));
            pieces.put(new Position(file, 8), new Knight(Color.BLACK));
        }

        for (char file : new char[]{'c', 'f'}) {
            pieces.put(new Position(file, 1), new Bishop(Color.WHITE));
            pieces.put(new Position(file, 8), new Bishop(Color.BLACK));
        }

        pieces.put(new Position('d', 1), new Queen(Color.WHITE));
        pieces.put(new Position('d', 8), new Queen(Color.BLACK));

        pieces.put(new Position('e', 1), new King(Color.WHITE));
        pieces.put(new Position('e', 8), new King(Color.BLACK));
    }

    public Piece getPiece(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position must be not null.");
        }

        return pieces.get(position);
    }

    public Map<Position, Piece> getPieces() {
        return new HashMap<>(pieces);
    }

    public Map<Position, Piece> getPiecesByColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color must be not null.");
        }

        Map<Position, Piece> result = new HashMap<>();
        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            if (entry.getValue().getColor() == color) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public boolean isEmpty(Position position) {
        return getPiece(position) == null;
    }

    public Position findKing(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color must be not null.");
        }

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Piece piece = entry.getValue();
            if (piece instanceof King && piece.getColor() == color) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("King not found on the board.");
    }

    public Board copy() {
        return new Board(getPieces());
    }

    public void undoMove(Map<Position, Piece> boardState) {
        if (boardState == null) {
            throw new IllegalArgumentException("Board state must be not null.");
        }
        this.pieces.clear();
        this.pieces.putAll(boardState);
    }

    public void placePiece(Position position, Piece piece) {
        if (position == null) {
            throw new IllegalArgumentException("Position must be not null.");
        }
        if (piece == null) {
            throw new IllegalArgumentException("Piece must be not null.");
        }
        pieces.put(position, piece);
    }

    public void removePiece(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position must be not null.");
        }
        pieces.remove(position);
    }

    public void movePiece(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("model.Move must be not null.");
        }

        Piece piece = getPiece(move.from());
        if (piece == null) {
            throw new IllegalArgumentException("No piece at the source position.");
        }

        removePiece(move.from());
        placePiece(move.to(), piece);
    }

}
