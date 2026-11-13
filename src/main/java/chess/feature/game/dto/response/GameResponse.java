package chess.feature.game.dto.response;

import chess.engine.model.piece.Color;

import java.time.Instant;

public record GameResponse(
        GameUpdateResponse gameUpdateResponse,
        // New fields for player IDs
        String whitePlayerId,
        String blackPlayerId,
        Color drawOfferedBy,

        Instant createdAt
) {
}