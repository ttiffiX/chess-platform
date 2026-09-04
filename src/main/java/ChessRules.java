import piece.*;
import java.util.ArrayList;

import java.util.List;

public class ChessRules {
    private static final PathService PATH_SERVICE = new PathService();
    private static final CheckService CHECK_SERVICE = new CheckService(PATH_SERVICE);
    private static final MoveValidator MOVE_VALIDATOR = new MoveValidator(PATH_SERVICE, CHECK_SERVICE);
    private static final LegalMoveService LEGAL_MOVE_SERVICE = new LegalMoveService(MOVE_VALIDATOR);
    private static final DrawService DRAW_SERVICE = new DrawService();

    public static ValidationResult validateMove(Board board, Move move, Color turn, List<MoveRecord> moveRecords) {
        List<MoveRecord> safeHistory = moveRecords == null ? new ArrayList<>() : moveRecords;
        return MOVE_VALIDATOR.validate(board, move, turn, safeHistory);
    }

    public static boolean isValidMove(Board board, Move move) {
        if (board == null || move == null) {
            return false;
        }

        Piece piece = board.getPiece(move.from());
        if (piece == null) {
            return false;
        }

        return MOVE_VALIDATOR.validate(board, move, piece.getColor(), List.of()).valid();
    }

    public static boolean isValidMove(Board board, Move move, List<MoveRecord> moveRecords) {
        if (board == null || move == null) {
            return false;
        }

        Piece piece = board.getPiece(move.from());
        if (piece == null) {
            return false;
        }

        List<MoveRecord> safeHistory = moveRecords == null ? new ArrayList<>() : moveRecords;
        return MOVE_VALIDATOR.validate(board, move, piece.getColor(), safeHistory).valid();
    }

    public static boolean isCheckmate(Board board, Color color) {
        return CHECK_SERVICE.isInCheck(board, color) && !LEGAL_MOVE_SERVICE.hasAnyLegalMove(board, color, List.of());
    }

    public static boolean isStalemate(Board board, Color color) {
        return !CHECK_SERVICE.isInCheck(board, color) && !LEGAL_MOVE_SERVICE.hasAnyLegalMove(board, color, List.of());
    }

    public static boolean hasLegalMoves(Board board, Color color, List<MoveRecord> history) {
        return LEGAL_MOVE_SERVICE.hasAnyLegalMove(board, color, history == null ? List.of() : history);
    }

    public static boolean isInCheck(Board board, Color color) {
        return CHECK_SERVICE.isInCheck(board, color);
    }

    public static boolean isInsufficientMaterial(Board board) {
        return DRAW_SERVICE.isInsufficientMaterial(board);
    }

    public static boolean isFiftyMoveRule(int halfMoveClock) {
        return DRAW_SERVICE.isFiftyMoveRule(halfMoveClock);
    }

    public static boolean isThreefoldRepetition(List<MoveRecord> history) {
        return DRAW_SERVICE.isThreefoldRepetition(history == null ? List.of() : history);
    }
}