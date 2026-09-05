package rules.validator;

import model.Board;
import model.Move;
import model.piece.Color;
import model.piece.King;
import model.piece.Pawn;
import model.piece.Piece;
import rules.*;
import rules.services.CheckService;
import rules.services.PathService;

public class PromotionRuleValidator implements MoveRuleValidator {
    private final PathService pathService;
    private final CheckService checkService;

    public PromotionRuleValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.pathService = pathService;
        this.checkService = checkService;
    }

    @Override
    public boolean isApplicable(ValidationContext ctx) {
        Piece piece = ctx.board().getPiece(ctx.move().from());
        if (!(piece instanceof Pawn)) {
            return false;
        }

        int targetRank = ctx.move().to().rank();
        return (ctx.turn() == Color.WHITE && targetRank == 8)
                || (ctx.turn() == Color.BLACK && targetRank == 1);
    }

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        Board board = ctx.board();
        Move move = ctx.move();
        Color turn = ctx.turn();

        Piece piece = board.getPiece(move.from());
        Piece targetPiece = board.getPiece(move.to());

        // 1. Phải có chỉ định quân phong cấp
        Piece promotedPiece = move.promotedPiece();
        if (promotedPiece == null) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_PROMOTION,
                    "Promotion requires a designated piece."
            );
        }

        // 2. Quân phong cấp phải cùng màu và không được là Vua hay Tốt
        if (promotedPiece.getColor() != turn
                || promotedPiece instanceof King
                || promotedPiece instanceof Pawn) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_PROMOTION,
                    "Invalid promotion piece choice."
            );
        }

        // 3. Không ăn quân mình
        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return ValidationResult.fail(
                    InvalidMoveReason.TARGET_HAS_ALLY_PIECE,
                    "Cannot capture your own piece."
            );
        }

        // 4. Hình học di chuyển của Tốt
        boolean legalPattern = (targetPiece == null)
                ? piece.canMove(move.from(), move.to())
                : piece.canCapture(move.from(), move.to());

        if (!legalPattern) {
            return ValidationResult.fail(
                    InvalidMoveReason.ILLEGAL_PATTERN,
                    "Piece movement pattern is illegal."
            );
        }

        // 5. Đường đi không bị chặn
        if (!pathService.isPathClear(board, move.from(), move.to())) {
            return ValidationResult.fail(
                    InvalidMoveReason.PATH_BLOCKED,
                    "Path is blocked."
            );
        }

        // 6. Kiểm tra an toàn của Vua
        if (checkService.leavesKingInCheck(board, move)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Move leaves your king in check."
            );
        }

        return ValidationResult.ok(b -> {
            b.removePiece(move.from());
            b.removePiece(move.to());
            b.placePiece(move.to(), promotedPiece);
        }, targetPiece);
    }
}