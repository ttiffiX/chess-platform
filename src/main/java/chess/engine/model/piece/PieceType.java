package chess.engine.model.piece;

public enum PieceType {
    PAWN("p"),
    KNIGHT("n"),
    BISHOP("b"),
    ROOK("r"),
    QUEEN("q"),
    KING("k");

    private final String uciChar;

    PieceType(String uciChar) {
        this.uciChar = uciChar;
    }

    public String getUciChar() {
        return this.uciChar;
    }

    public Piece getPiece(Color color) {
        return switch (this) {
            case PAWN -> new Pawn(color);
            case KNIGHT -> new Knight(color);
            case BISHOP -> new Bishop(color);
            case ROOK -> new Rook(color);
            case QUEEN -> new Queen(color);
            case KING -> new King(color);
        };
    }
}
