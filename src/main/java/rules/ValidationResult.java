package rules;

public record ValidationResult(boolean valid, InvalidMoveReason reason, String message) {
    public ValidationResult {
        if (valid && reason != null) {
            throw new IllegalArgumentException("Valid result cannot contain a reason.");
        }

        if (!valid && reason == null) {
            throw new IllegalArgumentException("Invalid result must contain a reason.");
        }

        if (!valid && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("Invalid result must contain a message.");
        }
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null, "OK");
    }

    public static ValidationResult fail(InvalidMoveReason reason, String message) {
        return new ValidationResult(false, reason, message);
    }
}
