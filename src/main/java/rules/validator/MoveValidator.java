package rules.validator;

import model.piece.Piece;
import rules.*;
import rules.services.CheckService;
import rules.services.PathService;

import java.util.List;

public class MoveValidator {
    private final List<MoveRuleValidator> rules;

    public MoveValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }

        // Đăng ký theo thứ tự ưu tiên rõ ràng
        this.rules = List.of(
                new CastlingRuleValidator(pathService, checkService),
                new EnPassantRuleValidator(checkService),
                new PromotionRuleValidator(pathService, checkService),
                new StandardMoveRuleValidator(pathService, checkService)
        );
    }

    public ValidationResult validate(ValidationContext ctx) {
        Piece piece = ctx.board().getPiece(ctx.move().from());

        if (piece == null) {
            return ValidationResult.fail(
                    InvalidMoveReason.NO_PIECE_AT_SOURCE,
                    "No piece at the source position."
            );
        }

        if (piece.getColor() != ctx.turn()) {
            return ValidationResult.fail(
                    InvalidMoveReason.WRONG_TURN,
                    "It's " + ctx.turn() + "'s turn."
            );
        }

        for (MoveRuleValidator rule : rules) {
            if (rule.isApplicable(ctx)) {
                return rule.validate(ctx);
            }
        }

        return ValidationResult.fail(
                InvalidMoveReason.ILLEGAL_PATTERN,
                "No valid rule matches this move."
        );
    }
}