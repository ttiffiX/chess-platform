import piece.Position;

public record Move(Position from, Position to) {
    public Move {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid move: from and to positions cannot be null");
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move: from and to positions are the same");
        }
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
