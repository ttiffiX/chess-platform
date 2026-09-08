package chess.engine.model;

public record Position(char file, int rank) {

    public Position {
        if (!(file >= 'a' && file <= 'h' && rank >= 1 && rank <= 8)) {
            throw new IllegalArgumentException("Invalid position: " + file + rank);
        }
    }

    @Override
    public String toString() {
        return "" + file + rank;
    }
}
