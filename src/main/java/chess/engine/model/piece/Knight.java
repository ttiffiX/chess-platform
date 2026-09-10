package chess.engine.model.piece;

import chess.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public Knight(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }

    @Override
    public List<Position> getCandidateDestinations(Position from) {
        List<Position> candidates = new ArrayList<>();
        for (int[] offset : OFFSETS) {
            addIfValid(candidates, (char) (from.file() + offset[0]), from.rank() + offset[1]);
        }
        return candidates;
    }
}
