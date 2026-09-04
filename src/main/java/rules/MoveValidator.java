import piece.Color;
import piece.Knight;
import piece.Piece;

import java.util.List;

public class MoveValidator {
    private final PathService pathService;
    private final CheckService checkService;

    public MoveValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.pathService = pathService;
        this.checkService = checkService;
    }

    public ValidationResult validate(Board board, Move move, Color turn, List<MoveRecord> history) {
        if (board == null || move == null || turn == null) {
            throw new IllegalArgumentException("Board, move and turn must not be null.");
        }

        Piece piece = board.getPiece(move.from());
        if (piece == null) {
            return ValidationResult.fail(
                    InvalidMoveReason.NO_PIECE_AT_SOURCE,
                    "No piece at the source position."
            );
        }

        if (piece.getColor() != turn) {
            return ValidationResult.fail(
                    InvalidMoveReason.WRONG_TURN,
                    "It's " + turn + "'s turn."
            );
        }

        Piece targetPiece = board.getPiece(move.to());
        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return ValidationResult.fail(
                    InvalidMoveReason.TARGET_HAS_ALLY_PIECE,
                    "Cannot capture your own piece."
            );
        }

        boolean legalPattern = targetPiece == null
                ? piece.canMove(move.from(), move.to())
                : piece.canCapture(move.from(), move.to());

        if (!legalPattern) {
            return ValidationResult.fail(
                    InvalidMoveReason.ILLEGAL_PATTERN,
                    "Piece movement pattern is illegal."
            );
        }

        if (!(piece instanceof Knight) && !pathService.isPathClear(board, move.from(), move.to())) {
            return ValidationResult.fail(
                    InvalidMoveReason.PATH_BLOCKED,
                    "Path is blocked."
            );
        }

        if (checkService.leavesKingInCheck(board, move)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Move leaves your king in check."
            );
        }

        return ValidationResult.ok();
    }
}
