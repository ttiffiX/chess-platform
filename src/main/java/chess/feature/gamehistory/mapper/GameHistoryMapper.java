package chess.feature.gamehistory.mapper;

import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.entity.GameHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameHistoryMapper {
    public GameHistory toHistory(GameSession gameSession) {
        return GameHistory.builder()
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
                .plyCount(gameSession.getPlyCount())
                .clocksMs(gameSession.getClocksMs())
                .startedAt(gameSession.getCreatedAt())
                .endedAt(gameSession.getEndedAt())
                .build();
    }
}
