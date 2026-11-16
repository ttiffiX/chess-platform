package chess.feature.game.session;

import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.piece.Color;
import chess.feature.game.model.PersistState;
import chess.feature.game.model.Record;
import chess.feature.game.model.TimeControl;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class GameSession {
    @Getter(AccessLevel.NONE)
    private final ReentrantLock lock = new ReentrantLock();

    private final String gameId;
    private final Game game;
    private final ChessClock clock;
    private final List<Record> history = new ArrayList<>();

    private final String whitePlayerId;
    private final String blackPlayerId;

    private Color drawOfferedBy;

    private long stateVersion; // Phiên bản trạng thái của game session, dùng để đồng bộ trạng thái giữa server và client.
    private volatile PersistState persistState = PersistState.NOT_STARTED; // volatile để đảm bảo tính nhất quán khi truy cập từ nhiều luồng.

    private final Instant createdAt;
    private Instant endedAt;

    public GameSession(TimeControl timeControl, String whitePlayerId, String blackPlayerId) {
        this.gameId = UUID.randomUUID().toString();
        this.game = new Game();
        this.createdAt = Instant.now();
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.clock = new ChessClock(timeControl);
        this.stateVersion = 0;
        this.history.add(new Record(
                null,
                getFEN(),
                null,
                game.isInCheck(),
                null
        ));
    }

    public <T> T locked(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void runLocked(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public List<Record> getHistory() {
        return new ArrayList<>(history);
    }

    public Record getLastHistory() {
        return history.getLast();
    }

    public void addHistory(String uci) {
        this.history.add(new Record(
                uci,
                getFEN(),
                game.getCapturedPiece(),
                game.isInCheck(),
                clock.isTimed() ? (int) clock.getStoredRemainingMs(game.getCurrentTurn().opposite()) : null
        ));
    }

    public String getFEN() {
        return game.getCurrentFEN();
    }

    public Map<String, List<String>> extractLegalMovesMap() {
        Map<String, List<String>> legalMovesMap = new HashMap<>();
        List<Move> legalMoves = game.getAllLegalMovesByColor();

        for (Move move : legalMoves) {
            legalMovesMap.computeIfAbsent(move.from().toString(), _ -> new ArrayList<>()).add(move.to().toString());
        }
        return legalMovesMap;
    }

    public void offerDraw(Color playerColor) {
        this.drawOfferedBy = playerColor;
    }

    public void clearDrawOffer() {
        this.drawOfferedBy = null;
    }

    public Color getPlayerColor(String playerId) {
        if (whitePlayerId.equals(playerId)) {
            return Color.WHITE;
        }

        if (blackPlayerId.equals(playerId)) {
            return Color.BLACK;
        }

        return null;
    }

    public void startClock(Instant now) {
        clock.start(game.getCurrentTurn(), now);
    }

    public void completeMove(Color moveColor, Instant moveReceivedAt, Instant nextTurnStartedAt) {
        clock.completeMove(moveColor, moveReceivedAt, nextTurnStartedAt);
    }

    public void stopClock(Instant now) {
        clock.stop(now);
    }

    public boolean checkTimeoutAndUpdate(Instant now) {
        if (game.isGameOver() || !clock.isTimedOut(now)) {
            return false;
        }
        game.isTimedOut(clock.getActiveColor());

        finalizeGameIfOver(now);
        return true;
    }

    public Long getTurnStartedAt() {
        return clock.getLastTimestamp() != null ? clock.getLastTimestamp().toEpochMilli() : null;
    }

    public Long getWhiteRemainingMs() {
        return clock.getWhiteRemainingMs();
    }

    public Long getBlackRemainingMs() {
        return clock.getBlackRemainingMs();
    }

    public boolean finalizeGameIfOver(Instant now) {
        if (!game.isGameOver()) {
            return false;
        }
        stopClock(now);
        clearDrawOffer();
        endedAt = now;
        return true;
    }

    public void incrementStateVersion() {
        stateVersion = stateVersion + 1;
    }

    public String getMovesToString() {
        return history.stream()
                .map(Record::move)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    public long[] getClocksMs() {
        if (!clock.isTimed()) {
            return null;
        }

        long[] clocksMs = new long[history.size() - 1];
        for (int i = 1; i < history.size(); i++) {
            Record record = history.get(i);
            clocksMs[i - 1] = record.moverClockAfterMs() != null ? record.moverClockAfterMs() : 0;
        }
        return clocksMs;
    }

    public int getPlyCount() {
        return history.size() - 1; // history[0] là trạng thái ban đầu, không tính vào ply count
    }

    public boolean beginPersist() {
        if (persistState != PersistState.NOT_STARTED) return false;
        persistState = PersistState.PENDING;
        return true;
    }

    public void markSaved() {
        persistState = PersistState.SAVED;
    }

    public void markFailed() {
        persistState = PersistState.FAILED;
    }

    public boolean isSaved() {
        return persistState == PersistState.SAVED;
    }
}