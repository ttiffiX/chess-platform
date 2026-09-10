package chess.engine.model.piece;

import chess.engine.model.Position;

import java.util.List;
import java.util.Objects;

public abstract class Piece {
    private final Color color;

    public Piece(Color color) {
        Objects.requireNonNull(color, "Color must not be null");
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    public abstract boolean canMove(Position from, Position to);

    public boolean canCapture(Position from, Position to) {
        return canMove(from, to);
    }

    public abstract PieceType getType();

    public abstract List<Position> getCandidateDestinations(Position from);

    protected void addIfValid(List<Position> list, char file, int rank) {
        if (Position.isValid(file, rank)) {
            list.add(new Position(file, rank));
        }
    }
}
