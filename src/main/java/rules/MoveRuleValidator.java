package rules;

public interface MoveRuleValidator {
    boolean isApplicable(ValidationContext ctx);
    ValidationResult validate(ValidationContext ctx);
}