package chess.engine.model;

public record Position(char file, int rank) {

    public Position {
        if (!isValid(file, rank)) {
            throw new IllegalArgumentException("Invalid position: " + file + rank);
        }
    }

    public static boolean isValid(char file, int rank) {
        return file >= 'a' && file <= 'h' && rank >= 1 && rank <= 8;
    }

    @Override
    public String toString() {
        return "" + file + rank;
    }
}
