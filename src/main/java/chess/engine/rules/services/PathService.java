package chess.engine.rules.services;

import chess.engine.model.Board;
import chess.engine.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PathService {
    public boolean isPathClear(Board board, Position from, Position to) {
        Objects.requireNonNull(board, "Board must not be null.");
        Objects.requireNonNull(from, "From position must not be null.");
        Objects.requireNonNull(to, "To position must not be null.");

        for (Position betweenSquare : between(from, to)) {
            if (!board.isEmpty(betweenSquare)) {
                return false;
            }
        }
        return true;
    }

    public List<Position> between(Position from, Position to) {
        Objects.requireNonNull(from, "From position must not be null.");
        Objects.requireNonNull(to, "To position must not be null.");

        int fileDelta = to.file() - from.file();
        int rankDelta = to.rank() - from.rank();

        int fileStep = Integer.compare(fileDelta, 0);
        int rankStep = Integer.compare(rankDelta, 0);

        if (fileStep == 0 && rankStep == 0) {
            return List.of();
        }

        boolean straight = fileDelta == 0 || rankDelta == 0;
        boolean diagonal = Math.abs(fileDelta) == Math.abs(rankDelta);
        if (!straight && !diagonal) {
            return List.of();
        }

        List<Position> result = new ArrayList<>();
        char currentFile = (char) (from.file() + fileStep);
        int currentRank = from.rank() + rankStep;
        while (currentFile != to.file() || currentRank != to.rank()) {
            result.add(new Position(currentFile, currentRank));
            currentFile = (char) (currentFile + fileStep);
            currentRank += rankStep;
        }

        return result;
    }
}
