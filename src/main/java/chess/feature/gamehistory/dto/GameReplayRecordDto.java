package chess.feature.gamehistory.dto;

import java.util.List;
import java.util.Map;

public record GameReplayRecordDto(
        String move,
        String fen,
        String capturedPiece,
        boolean inCheck,
        Integer moverClockAfterMs,
        Map<String, List<String>> legalMoves
) {
}
