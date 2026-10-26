package chess.feature.game.session;

import chess.engine.model.piece.Color;
import chess.feature.game.model.TimeControl;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Getter
public class ChessClock {
    private final TimeControl timeControl;

    private long whiteRemainingMs;
    private long blackRemainingMs;

    private Color activeColor;
    private Instant lastTimestamp;
    private boolean running;

    public ChessClock(TimeControl timeControl) {
        this.timeControl = Objects.requireNonNull(
                timeControl,
                "Time control must not be null."
        );
        this.whiteRemainingMs = timeControl.baseTimeMs();
        this.blackRemainingMs = timeControl.baseTimeMs();
        this.running = false;
    }

    public boolean isTimed() {
        return timeControl.isTimed();
    }

    public void start(Color firstTurn, Instant now) {
        Objects.requireNonNull(firstTurn, "First turn must not be null.");
        Objects.requireNonNull(now, "Timestamp must not be null.");

        if (!isTimed()) {
            return;
        }

        if (running || lastTimestamp != null) {
            throw new IllegalStateException(
                    "Clock has already been started."
            );
        }

        this.activeColor = firstTurn;
        this.lastTimestamp = now;
        this.running = true;
    }

    /**
     * Completes a move made by moveColor.
     * Returns false if the clock is not running, the color
     * is not active, or the player's time has expired.
     */
    public void completeMove(Color moveColor, Instant moveReceivedAt,
                             Instant nextTurnStartedAt) {
        Objects.requireNonNull(moveColor, "Move color must not be null.");
        Objects.requireNonNull(moveReceivedAt, "Timestamp must not be null.");
        Objects.requireNonNull(nextTurnStartedAt, "Timestamp must not be null.");

        if (!isTimed()) {
            return;
        }

        if (!running) {
            throw new IllegalStateException("Clock is not running.");
        }

        if (moveColor != activeColor) {
            throw new IllegalStateException("Move color does not match active clock color.");
        }

        long elapsedMs = elapsedSinceLastTimestamp(moveReceivedAt);

        long remainingMs = getStoredRemainingMs(moveColor);

        if (elapsedMs >= remainingMs) {
            throw new IllegalStateException(
                    "Cannot complete move after timeout. Timeout must be handled before applying the move."
            );
        }

        setRemainingMs(
                moveColor,
                remainingMs - elapsedMs + timeControl.incrementMs()
        );

        activeColor = moveColor.opposite();
        lastTimestamp = nextTurnStartedAt;
    }

    public boolean isTimedOut(Instant now) {
        Objects.requireNonNull(now, "Timestamp must not be null.");

        if (!isTimed() || !running || activeColor == null) {
            return false;
        }

        long elapsedMs = elapsedSinceLastTimestamp(now);

        return elapsedMs >= getStoredRemainingMs(activeColor);
    }

    /**
     * Settles the active player's elapsed time and stops the clock.
     */
    public void stop(Instant now) {
        Objects.requireNonNull(now, "Timestamp must not be null.");

        if (!running) {
            return;
        }

        long elapsedMs = elapsedSinceLastTimestamp(now);
        long remainingMs = getStoredRemainingMs(activeColor);

        setRemainingMs(
                activeColor,
                Math.max(0, remainingMs - elapsedMs)
        );

        lastTimestamp = now;
        running = false;
    }

    private long elapsedSinceLastTimestamp(Instant now) {
        if (now.isBefore(lastTimestamp)) {
            throw new IllegalArgumentException(
                    "Timestamp must not be before the last timestamp."
            );
        }

        return Duration.between(lastTimestamp, now).toMillis();
    }

    private long getStoredRemainingMs(Color color) {
        return color == Color.WHITE
                ? whiteRemainingMs
                : blackRemainingMs;
    }

    private void setRemainingMs(Color color, long remainingMs) {
        if (color == Color.WHITE) {
            whiteRemainingMs = remainingMs;
        } else {
            blackRemainingMs = remainingMs;
        }
    }
}