package chess.engine.model.piece;

import chess.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

public class Bishop extends Piece {
    private static final int[][] DIRECTIONS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    public Bishop(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return fileDiff == rankDiff && fileDiff != 0;
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }

    @Override
    public List<Position> getCandidateDestinations(Position from) {
        List<Position> candidates = new ArrayList<>();
        for (int[] dir : DIRECTIONS) {
            char f = (char) (from.file() + dir[0]);
            int r = from.rank() + dir[1];
            while (Position.isValid(f, r)) {
                candidates.add(new Position(f, r));
                f += (char) dir[0];
                r += dir[1];
            }
        }
        return candidates;
    }
}
