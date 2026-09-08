package chess.chess_engine.rules.services;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.Position;

import java.util.ArrayList;
import java.util.List;

public class PathService {
    public boolean isPathClear(Board board, Position from, Position to) {
        if (board == null || from == null || to == null) {
            throw new IllegalArgumentException("Board and positions must not be null.");
        }

        for (Position betweenSquare : between(from, to)) {
            if (!board.isEmpty(betweenSquare)) {
                return false;
            }
        }
        return true;
    }

    public List<Position> between(Position from, Position to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Positions must not be null.");
        }

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
