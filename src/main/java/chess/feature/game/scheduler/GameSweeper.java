package chess.feature.game.scheduler;

import chess.feature.game.dto.response.GameEventResponse;
import chess.feature.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSweeper {
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelayString = "${app.game.sweep-tick-ms:5000}")
    public void sweep() {
        Instant now = Instant.now();
        try {
            // 1. Ván nào hết giờ thì kết thúc, rồi báo cho người đang online
            for (GameEventResponse ended : gameService.finishExpiredGames(now)) {
                messagingTemplate.convertAndSend("/topic/game/" + ended.gameId(), ended);
            }

            // 2. Dọn session đã lưu và hết grace
            int removed = gameService.evictExpired(now);
            if (removed > 0) log.info("Evicted {} finished game sessions", removed);
        } catch (Exception e) {
            log.error("Game sweep failed", e);
        }
    }
}