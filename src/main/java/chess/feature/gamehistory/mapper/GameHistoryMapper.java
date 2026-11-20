package chess.feature.gamehistory.mapper;

import chess.engine.model.piece.Color;
import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.model.GameOutcome;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import chess.feature.user.dto.PlayerSummary;
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

    public GameHistoryDto toGameHistoryDto(
            GameHistory history,
            PlayerSummary opponent,
            Color playerColor,
            GameOutcome outcome
    ) {
        MatchmakingTimeControlPreset timeControlPreset = MatchmakingTimeControlPreset.fromBaseAndIncrement(
                history.getBaseSeconds(),
                history.getIncrementSeconds()
        );

        return new GameHistoryDto(
                history.getGameId(),
                opponent,
                playerColor,
                history.getStatus(),
                outcome,
                timeControlPreset,
                history.getPlyCount(),
                history.getStartedAt(),
                history.getEndedAt()
        );
    }
}
