package chess.feature.game.model;

public record TimeControl(
        long baseTimeSeconds,
        long incrementSeconds
) {
    public TimeControl {
        if (baseTimeSeconds < 0) {
            throw new IllegalArgumentException(
                    "Base time must not be negative."
            );
        }

        if (incrementSeconds < 0) {
            throw new IllegalArgumentException(
                    "Increment must not be negative."
            );
        }

        if (baseTimeSeconds == 0 && incrementSeconds != 0) {
            throw new IllegalArgumentException(
                    "Untimed games cannot have increment."
            );
        }
    }

    public boolean isTimed() {
        return baseTimeSeconds > 0;
    }

    public long baseTimeMs() {
        return Math.multiplyExact(baseTimeSeconds, 1_000L);
    }

    public long incrementMs() {
        return Math.multiplyExact(incrementSeconds, 1_000L);
    }

    public static TimeControl untimed() {
        return new TimeControl(0, 0);
    }
}