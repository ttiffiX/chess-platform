package rules;

import model.Board;
import model.Move;
import piece.Knight;
import piece.Piece;

public class StandardMoveRuleValidator implements MoveRuleValidator {
    private final PathService pathService;
    private final CheckService checkService;

    public StandardMoveRuleValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.pathService = pathService;
        this.checkService = checkService;
    }

    @Override
    public boolean isApplicable(ValidationContext ctx) {
        return true;
    }

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        Board board = ctx.board();
        Move move = ctx.move();

        Piece piece = board.getPiece(move.from());
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

        return ValidationResult.ok(b -> b.movePiece(move), targetPiece);
    }
}
