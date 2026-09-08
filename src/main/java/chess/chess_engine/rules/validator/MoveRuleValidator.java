package chess.chess_engine.rules.validator;

import chess.chess_engine.rules.ValidationContext;
import chess.chess_engine.rules.ValidationResult;

public interface MoveRuleValidator {
    boolean isApplicable(ValidationContext ctx);
    ValidationResult validate(ValidationContext ctx);
}