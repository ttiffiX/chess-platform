package chess.engine.utils;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Pawn;

import java.util.List;

public class EnPassantUtils {
    public static String createEnPassantTargetKey(Position enPassantTarget) {
        return enPassantTarget != null ? enPassantTarget.toString() : "-";
    }

    public static String createEnPassantTargetKey(
            Board board,
            Position enPassTarget,
            List<Move> legalMoves
    ) {
        if (enPassTarget == null || !board.isEmpty(enPassTarget)) {
            return "-";
        }

        return legalMoves.stream().anyMatch(move -> {
            if (!move.to().equals(enPassTarget)) {
                return false;
            }

            return board.getPiece(move.from()) instanceof Pawn;
        }) ? enPassTarget.toString() : "-";
    }
}
