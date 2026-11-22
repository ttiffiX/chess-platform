package chess.feature.analysis.dto;

import chess.engine.model.game.GameResult;

public record GameAnalysisDto(
        GameResult gameResult,
        GameReplayRecordDto records
) {
}
