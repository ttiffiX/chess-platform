package chess.feature.gamehistory.service;

import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameHistoryService {
    private final GameHistoryRepository gameHistoryRepository;

    @Transactional
    public void saveGameHistory(GameSession gameSession) {
        gameHistoryRepository.save(new GameHistory().toBuilder()
                .gameId(gameSession.getGameId())
                .schemaVersion(1)
                .whitePlayerId(gameSession.getWhitePlayerId())
                .blackPlayerId(gameSession.getBlackPlayerId())
                .playerIds(List.of(gameSession.getWhitePlayerId(), gameSession.getBlackPlayerId()))
                .status(gameSession.getGame().getGameResult().status())
                .winner(gameSession.getGame().getGameResult().winner())
                .baseSeconds(gameSession.getClock().getTimeControl().baseTimeSeconds())
                .incrementSeconds(gameSession.getClock().getTimeControl().incrementSeconds())
                .moves(gameSession.getMovesToString())
                .plyCount(gameSession.getHistory().size())
                .clocksMs(gameSession.getClocksMs())
                .startedAt(gameSession.getCreatedAt())
                .endedAt(gameSession.getEndedAt())
                .build());
    }

    public GameHistory getGameHistory(String gameId) {
        // Lấy lịch sử trò chơi từ cơ sở dữ liệu hoặc nơi lưu trữ khác.
        return null;
    }
}
