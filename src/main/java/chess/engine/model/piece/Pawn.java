package chess.engine.model.piece;

import chess.engine.model.Position;

public class Pawn extends Piece {
    public Pawn(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = to.rank() - from.rank();

        if (getColor() == Color.WHITE) {
            return (fileDiff == 0 && rankDiff == 1) || (fileDiff == 0 && from.rank() == 2 && rankDiff == 2);
        } else {
            return (fileDiff == 0 && rankDiff == -1) || (fileDiff == 0 && from.rank() == 7 && rankDiff == -2);
        }
    }

    @Override
    public boolean canCapture(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = to.rank() - from.rank();

        if (getColor() == Color.WHITE) {
            return fileDiff == 1 && rankDiff == 1;
        } else {
            return fileDiff == 1 && rankDiff == -1;
        }
    }
}
