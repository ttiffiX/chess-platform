package chess.engine.model.piece;

import chess.engine.model.Position;

public class Rook extends Piece {
    public Rook(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return (fileDiff == 0 || rankDiff == 0) && !(fileDiff == 0 && rankDiff == 0);
    }

    @Override
    public PieceType getType() {
        return PieceType.ROOK;
    }
}
