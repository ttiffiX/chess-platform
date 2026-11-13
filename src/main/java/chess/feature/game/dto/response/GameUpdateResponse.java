package chess.feature.game.dto.response;

import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import chess.feature.game.model.Record;

import java.util.List;
import java.util.Map;

public record GameUpdateResponse(
        String gameId,
        GameResult gameResult,
        boolean inCheck,
        String fen,
        Record historyItems,
        Map<String, List<String>> legalMoves,

        Long whiteRemainingMs,
        Long blackRemainingMs,
        Long turnStartTimeEpochMs,
        Long serverTimeEpochMs,

        Color drawOfferedBy,

        long stateVersion // Phiên bản trạng thái của game session, dùng để đồng bộ trạng thái giữa server và client.
) {
}