package rules.services;

import model.Board;
import model.piece.Bishop;
import model.piece.King;
import model.piece.Knight;
import model.piece.Piece;

import java.util.List;

public class DrawService {
    public boolean isInsufficientMaterial(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board must not be null.");
        }

        List<Piece> nonKingPieces = board.getPieces()
                .values()
                .stream()
                .filter(piece -> !(piece instanceof King))
                .toList();

        if (nonKingPieces.isEmpty()) {
            return true;
        }

        if (nonKingPieces.size() == 1) {
            Piece onlyPiece = nonKingPieces.getFirst();
            return onlyPiece instanceof Bishop || onlyPiece instanceof Knight;
        }

        return false;
    }

//    public boolean isFiftyMoveRule(int halfMoveClock) {
//        return halfMoveClock >= 100;
//    }
//
//    public boolean isThreefoldRepetition(List<MoveRecord> history) {
//        if (history == null) {
//            throw new IllegalArgumentException("History must not be null.");
//        }
//
//        Map<String, Integer> countByMove = new HashMap<>();
//        for (MoveRecord moveRecord : history) {
//            String key = moveRecord.move().toString();
//            int updatedCount = countByMove.getOrDefault(key, 0) + 1;
//            countByMove.put(key, updatedCount);
//            if (updatedCount >= 3) {
//                return true;
//            }
//        }
//
//        return false;
//    }
}
