package chess.feature.game.dto.response;

import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record GameEventResponse(
        String gameId,
        GameResult gameResult,

        @JsonInclude(NON_NULL)
        Color drawOfferedBy,

        long stateVersion
) {
}
