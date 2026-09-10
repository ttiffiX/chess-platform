package chess.engine.model.piece;

import chess.engine.model.Position;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public PieceType getType() {
        return PieceType.PAWN;
    }

    @Override
    public List<Position> getCandidateDestinations(Position from) {
        List<Position> candidates = new ArrayList<>();
        int direction = (getColor() == Color.WHITE) ? 1 : -1;
        int startRank = (getColor() == Color.WHITE) ? 2 : 7;

        // 1. Tiến 1 ô
        addIfValid(candidates, from.file(), from.rank() + direction);

        // 2. Tiến 2 ô từ vị trí ban đầu
        if (from.rank() == startRank) {
            addIfValid(candidates, from.file(), from.rank() + 2 * direction);
        }

        // 3. Ăn chéo sang trái và phải (kể cả trường hợp en passant)
        addIfValid(candidates, (char) (from.file() - 1), from.rank() + direction);
        addIfValid(candidates, (char) (from.file() + 1), from.rank() + direction);

        return candidates;
    }
}
