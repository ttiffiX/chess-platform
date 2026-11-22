package chess.feature.analysis.dto;

import chess.engine.model.game.GameResult;

import java.time.Instant;
import java.util.List;

public record GameReplayDto(
        String gameId,
        String whitePlayerId,
        String blackPlayerId,
        GameResult gameResult,
        long baseSeconds,
        long incrementSeconds,
        int plyCount,
        Instant startedAt,
        Instant endedAt,
        List<GameReplayRecordDto> records
) {
}
