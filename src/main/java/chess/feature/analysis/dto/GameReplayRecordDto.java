package chess.feature.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record GameReplayRecordDto(
        String move,
        String fen,

        @JsonInclude(NON_NULL)
        String capturedPiece,

        boolean inCheck,
        Integer moverClockAfterMs,
        Map<String, List<String>> legalMoves
) {
}
