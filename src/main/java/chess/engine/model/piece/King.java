package chess.engine.model.piece;

import chess.engine.model.Position;

public class King extends Piece {
    public King(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return Math.max(fileDiff, rankDiff) == 1;
    }

    @Override
    public PieceType getType() {
        return PieceType.KING;
    }
}
