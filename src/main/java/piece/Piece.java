package piece;

import model.Position;

public abstract class Piece {
    private final Color color;

    public Piece(Color color) {
        this.color = color;

        if (color == null) {
            throw new IllegalArgumentException("Color must be not null.");
        }
    }

    public Color getColor() {
        return this.color;
    }

    public abstract boolean canMove(Position from, Position to);

    public boolean canCapture(Position from, Position to) {
        return canMove(from, to);
    }
}
