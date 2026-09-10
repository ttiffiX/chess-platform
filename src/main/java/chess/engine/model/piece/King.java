package chess.engine.model.piece;

import chess.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    private static final int[][] OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

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

    @Override
    public List<Position> getCandidateDestinations(Position from) {
        List<Position> candidates = new ArrayList<>();
        // 8 ô xung quanh
        for (int[] o : OFFSETS) {
            addIfValid(candidates, (char) (from.file() + o[0]), from.rank() + o[1]);
        }
        // 2 ô nhập thành tiềm năng (phía cánh Hậu ô 'c', cánh Vua ô 'g')
        addIfValid(candidates, 'c', from.rank());
        addIfValid(candidates, 'g', from.rank());
        return candidates;
    }
}
