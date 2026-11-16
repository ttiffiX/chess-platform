package chess.feature.game.event;

import chess.feature.game.session.GameSession;

public record GameFinishedEvent(GameSession gameSession) {
}
