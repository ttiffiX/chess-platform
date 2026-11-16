package chess.feature.gamehistory.model;

import chess.feature.game.session.GameSession;
import chess.feature.gamehistory.entity.GameHistory;

import java.time.Instant;

public record RetryContext(GameSession gameSession, GameHistory gameHistory,
                           int attempts, Instant firstAt, Instant nextAt) {
}
