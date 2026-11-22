package chess.feature.analysis.dto;

import chess.feature.game.dto.request.MoveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GameAnalysisRequest(
        @NotBlank(message = "FEN cannot be blank")
        String fen,

        @NotNull(message = "Move cannot be null")
        @Valid
        MoveRequest move
) {
}