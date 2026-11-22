package chess.feature.analysis.dto;

import chess.engine.model.game.GameResult;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record GameAnalysisDto(
        GameResult gameResult,
        String move,
        String fen,

        @JsonInclude(NON_NULL)
        String capturedPiece,

        boolean inCheck,
        Map<String, List<String>> legalMoves
) {
}
