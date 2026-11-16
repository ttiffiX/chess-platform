package chess.feature.gamehistory.service;

import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.model.RetryContext;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.MappingException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameHistoryPersister {
    private static final int MAX_ATTEMPTS = 8;
    private static final Duration MAX_AGE = Duration.ofMinutes(30);
    private static final long BASE_MS = 1_000, MAX_MS = 60_000;

    private final GameHistoryService historyService;
    private final Map<String, RetryContext> retryMap = new ConcurrentHashMap<>();   // sổ chờ
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1000));

    /**
     * Chỉ đưa việc vào hàng đợi rồi về ngay.
     */
    public void submit(GameSession gameSession, GameHistory gameHistory) {
        Instant now = Instant.now();
        RetryContext first = new RetryContext(gameSession, gameHistory, 0, now, now);
        try {
            executor.execute(() -> attempt(first));
        } catch (RejectedExecutionException e) {
            retryMap.put(gameHistory.getGameId(), first);   // hàng đợi đầy: để scheduler lo, không làm hỏng nước đi cuối
        }
    }

    /**
     * Scheduler gọi mỗi vài giây.
     */
    public void retryDue() {
        Instant now = Instant.now();
        for (RetryContext context : retryMap.values()) {
            if (context.nextAt().isAfter(now)) continue;
            if (retryMap.remove(context.gameHistory().getGameId(), context)) {   // chỉ một thread giành được
                try {
                    executor.execute(() -> attempt(context));
                } catch (RejectedExecutionException e) {
                    retryMap.put(context.gameHistory().getGameId(), context);
                }
            }
        }
    }

    private void attempt(RetryContext context) {
        String id = context.gameHistory().getGameId();
        try {
            historyService.saveGameHistory(context.gameHistory());
            context.gameSession().markSaved();
            retryMap.remove(id);
        } catch (Exception e) {
            int attempts = context.attempts() + 1;
            Instant now = Instant.now();
            boolean giveUp = isPermanent(e) || attempts >= MAX_ATTEMPTS
                    || Duration.between(context.firstAt(), now).compareTo(MAX_AGE) > 0;
            if (giveUp) {
                context.gameSession().markFailed();
                retryMap.remove(id);
                log.error("GAME HISTORY NOT SAVED id={} attempts={} gameHistory={}", id, attempts, context.gameHistory(), e);
                return;
            }
            retryMap.put(id, new RetryContext(context.gameSession(), context.gameHistory(), attempts, context.firstAt(),
                    now.plusMillis(backoffMs(attempts))));
            log.warn("Save game history failed id={} attempt={}", id, attempts, e);
        }
    }

    private boolean isPermanent(Exception e) {   // chỉ liệt kê lỗi chắc chắn thử lại vô ích, bổ sung dần
        return e instanceof IllegalArgumentException
                || e instanceof MappingException
                || e instanceof ConversionException;
    }

    private long backoffMs(int attempts) {   // 1s, 2s, 4s… trần 60s, cộng jitter
        long ms = Math.min(MAX_MS, BASE_MS * (1L << Math.min(attempts - 1, 10)));
        return ms + ThreadLocalRandom.current().nextLong(ms / 4 + 1);
    }

    public int pendingCount() {
        return retryMap.size();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        retryMap.values().forEach(this::attempt);   // thử lần cuối trước khi tắt
    }
}
