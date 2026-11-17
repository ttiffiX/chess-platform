package chess.feature.gamehistory.dto;

import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.feature.gamehistory.enums.GameOutcome;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import chess.feature.user.dto.PlayerSummary;
import lombok.Builder;

import java.time.Instant;

@Builder()
public record GameHistoryDto(
        String gameId,
        PlayerSummary opponent,
        Color playerColor,
        GameStatus gameStatus,
        GameOutcome outcome,
        MatchmakingTimeControlPreset timeControlPreset,
        int plyCount,
        Instant startedAt,
        Instant endedAt
) {
}
