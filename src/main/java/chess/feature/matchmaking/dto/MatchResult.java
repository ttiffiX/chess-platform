package chess.feature.matchmaking.dto;

import chess.feature.game.session.GameSession;

public record MatchResult(boolean matched, String opponentId, GameSession session) {
}

