package chess.session;

import chess.dto.response.ClockSnapshot;
import chess.engine.model.piece.Color;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ChessClock {
    private final long baseTimeMs;
    private final long incrementMs;
    private long whiteRemainingMs;
    private long blackRemainingMs;
    private Instant lastMoveTimestamp;
    private boolean isRunning;

    private final List<ClockSnapshot> history = new ArrayList<>();

    public ChessClock(long baseTimeMs, long incrementMs) {
        this.baseTimeMs = baseTimeMs;
        this.incrementMs = incrementMs;
        this.whiteRemainingMs = baseTimeMs;
        this.blackRemainingMs = baseTimeMs;
        this.isRunning = false;
        this.lastMoveTimestamp = null;
        this.history.add(new ClockSnapshot(whiteRemainingMs, blackRemainingMs, null, false));
    }

    public static ChessClock untimed() {
        return new ChessClock(0, 0);
    }

    public boolean isTimed() {
        return baseTimeMs > 0;
    }

    public void onMove(Color moveColor, Instant now) {
        if (!isTimed()) {
            return;
        }

        // Nước đầu tiên của ván: bắt đầu kích hoạt đồng hồ
        if (!isRunning) {
            isRunning = true;
            lastMoveTimestamp = now;
            return;
        }

        // Tính thời gian đã tiêu tốn
        long elapsedMs = Duration.between(lastMoveTimestamp, now).toMillis();

        if (moveColor == Color.WHITE) {
            whiteRemainingMs = Math.max(0, whiteRemainingMs - elapsedMs + incrementMs);
        } else {
            blackRemainingMs = Math.max(0, blackRemainingMs - elapsedMs + incrementMs);
        }

        lastMoveTimestamp = now;
        history.add(new ClockSnapshot(whiteRemainingMs, blackRemainingMs, lastMoveTimestamp, true));
    }

    public boolean undo(int steps) {
        if (!isTimed() || history.size() <= steps) {
            return false;
        }

        for (int i = 0; i < steps; i++) {
            history.removeLast();
        }
        ClockSnapshot prev = history.getLast(); // Lấy snapshot của nước trước

        this.whiteRemainingMs = prev.whiteRemainingMs();
        this.blackRemainingMs = prev.blackRemainingMs();
        this.lastMoveTimestamp = prev.lastMoveTimestamp();
        this.isRunning = prev.isRunning();

        return true;
    }

    public boolean isTimedOut(Color currentTurn, Instant now) {
        if (!isTimed() || !isRunning || lastMoveTimestamp == null) {
            return false;
        }
        long elapsedMs = Duration.between(lastMoveTimestamp, now).toMillis();
        if (currentTurn == Color.WHITE) {
            return (whiteRemainingMs - elapsedMs) <= 0;
        } else {
            return (blackRemainingMs - elapsedMs) <= 0;
        }
    }

    public void stop() {
        this.isRunning = false;
    }
}