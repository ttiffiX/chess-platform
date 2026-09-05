package model.piece;

import model.Position;

public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return fileDiff == rankDiff && fileDiff != 0;
    }
}
