package chess.engine.rules.services;

import chess.engine.model.Board;
import chess.engine.model.Position;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.piece.Bishop;
import chess.engine.model.piece.King;
import chess.engine.model.piece.Knight;
import chess.engine.model.piece.Piece;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DrawService {
    public boolean isInsufficientMaterial(Board board) {
        Objects.requireNonNull(board, "Board must not be null");

        List<Map.Entry<Position, Piece>> nonKingEntries = board.getPieces().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof King))
                .toList();

        if (nonKingEntries.isEmpty()) {
            return true;
        }

        if (nonKingEntries.size() == 1) {
            Piece onlyPiece = nonKingEntries.getFirst().getValue();
            return onlyPiece instanceof Bishop || onlyPiece instanceof Knight;
        }

        if (nonKingEntries.size() == 2) {
            Map.Entry<Position, Piece> first = nonKingEntries.get(0);
            Map.Entry<Position, Piece> second = nonKingEntries.get(1);

            if (first.getValue() instanceof Bishop && second.getValue() instanceof Bishop) {
                Position firstPosition = first.getKey();
                Position secondPosition = second.getKey();
                return isLightSquare(firstPosition) == isLightSquare(secondPosition);
            }
        }

        return false;
    }

    private boolean isLightSquare(Position pos) {
        return ((pos.file() - 'a') + pos.rank()) % 2 != 0;
    }

    public boolean isFiftyMoveRule(int halfMoveClock) {
        return halfMoveClock >= 100;
    }

    public boolean isThreefoldRepetition(List<GameStateSnapshot> history) {
        Objects.requireNonNull(history, "History must not be null");

        Map<String, Integer> stateCounts = new HashMap<>();
        for (GameStateSnapshot snapshot : history) {
            String key = snapshot.stateKey();
            int updatedCount = stateCounts.getOrDefault(key, 0) + 1;
            stateCounts.put(key, updatedCount);

            if (updatedCount >= 3) {
                return true;
            }
        }


        return false;
    }
}
