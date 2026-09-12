package chess.dto.response;

import java.time.Instant;

public record ClockSnapshot(
        long whiteRemainingMs,
        long blackRemainingMs,
        Instant lastMoveTimestamp,
        boolean isRunning
) {
}
