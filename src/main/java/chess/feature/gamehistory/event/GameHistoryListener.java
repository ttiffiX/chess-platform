package chess.feature.gamehistory.event;

import chess.feature.game.event.GameFinishedEvent;
import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.mapper.GameHistoryMapper;
import chess.feature.gamehistory.service.GameHistoryPersister;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameHistoryListener {
    private final GameHistoryMapper mapper;
    private final GameHistoryPersister persister;

    @EventListener   // mặc định đồng bộ, chạy trong khóa session. Không thêm @Async
    public void onGameFinished(GameFinishedEvent event) {
        GameSession session = event.gameSession();
        if (!session.beginPersist()) return;
        try {
            persister.submit(session, mapper.toHistory(session));
        } catch (Exception e) {
            session.markFailed();    // lỗi ở đây không được phá luồng kết thúc ván
            log.error("Cannot prepare history for game {}", session.getGameId(), e);
        }
    }
}
