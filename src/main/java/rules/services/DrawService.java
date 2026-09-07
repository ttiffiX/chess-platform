package rules.services;

import model.Board;
import model.Position;
import model.game.GameStateSnapshot;
import model.piece.Bishop;
import model.piece.King;
import model.piece.Knight;
import model.piece.Piece;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawService {
    public boolean isInsufficientMaterial(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board must not be null.");
        }

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
        if (history == null) {
            throw new IllegalArgumentException("History must not be null.");
        }

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
