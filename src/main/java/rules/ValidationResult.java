package rules;

import model.Board;
import piece.Piece;

import java.util.function.Consumer;

public record ValidationResult(boolean valid, InvalidMoveReason reason, String message, Consumer<Board> execute, Piece capturedPiece) {
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

        if (!valid && execute != null) {
            throw new IllegalArgumentException("Invalid result cannot contain an execute action.");
        }

        if (valid && execute == null) {
            throw new IllegalArgumentException("Valid result must contain an execute action.");
        }

        if (!valid && capturedPiece != null) {
            throw new IllegalArgumentException("Invalid result cannot contain a captured piece.");
        }
    }

    public static ValidationResult ok(Consumer<Board> execute, Piece capturedPiece) {
        return new ValidationResult(true, null, "OK", execute, capturedPiece);
    }

    public static ValidationResult fail(InvalidMoveReason reason, String message) {
        return new ValidationResult(false, reason, message, null, null);
    }
}
