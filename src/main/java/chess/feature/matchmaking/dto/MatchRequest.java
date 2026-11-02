package chess.feature.matchmaking.dto;

import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import jakarta.validation.constraints.NotNull;

public record MatchRequest(
        @NotNull(message = "Time control preset must not be null")
        MatchmakingTimeControlPreset preset
) {
}