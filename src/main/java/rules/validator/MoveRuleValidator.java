package rules.validator;

import rules.ValidationContext;
import rules.ValidationResult;

public interface MoveRuleValidator {
    boolean isApplicable(ValidationContext ctx);
    ValidationResult validate(ValidationContext ctx);
}