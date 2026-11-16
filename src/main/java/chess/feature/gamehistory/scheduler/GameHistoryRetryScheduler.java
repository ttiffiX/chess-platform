package chess.feature.gamehistory.scheduler;

import chess.feature.gamehistory.service.GameHistoryPersister;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameHistoryRetryScheduler {
    private final GameHistoryPersister persister;

    @Scheduled(fixedDelayString = "${app.game.retry-tick-ms:5000}")
    public void retry() {
        try {
            persister.retryDue();
            int pending = persister.pendingCount();
            if (pending > 0) log.warn("{} game histories waiting for retry", pending);
        } catch (Exception e) {
            log.error("Game history retry tick failed", e);
        }
    }
}
