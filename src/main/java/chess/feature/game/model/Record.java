package chess.feature.game.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record Record(
        String move,
        String fen,

        @JsonInclude(NON_NULL)
        String capturedPiece,

        boolean inCheck,
        Integer moverClockAfterMs
) {
}
