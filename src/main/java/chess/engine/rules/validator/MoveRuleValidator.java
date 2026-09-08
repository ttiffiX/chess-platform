package chess.engine.rules.validator;

import chess.engine.rules.ValidationContext;
import chess.engine.rules.ValidationResult;

public interface MoveRuleValidator {
    boolean isApplicable(ValidationContext ctx);
    ValidationResult validate(ValidationContext ctx);
}