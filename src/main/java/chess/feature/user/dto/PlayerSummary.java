package chess.feature.user.dto;

import lombok.Builder;

@Builder
public record PlayerSummary(
        String playerId,
        String username,
        String avatarUrl
) {
}
