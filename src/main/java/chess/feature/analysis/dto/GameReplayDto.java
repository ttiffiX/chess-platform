package chess.feature.analysis.dto;

import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import chess.feature.user.dto.PlayerSummary;

import java.time.Instant;
import java.util.List;

public record GameReplayDto(
        String gameId,
        PlayerSummary opponent,
        Color playerColor,
        GameResult gameResult,
        long baseSeconds,
        long incrementSeconds,
        int plyCount,
        Instant startedAt,
        Instant endedAt,
        List<GameReplayRecordDto> records
) {
}
