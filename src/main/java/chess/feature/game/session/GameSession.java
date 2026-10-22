package chess.feature.game.session;

import chess.engine.model.game.Game;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class GameSession {
    private final String gameId;
    private final Game game;
    private final ChessClock clock;
    private final String whitePlayerId;
    private final String blackPlayerId;
    private final Instant createdAt;

    public GameSession(long baseTimeSeconds, long incrementSeconds, String whitePlayerId, String blackPlayerId) {
        this.gameId = UUID.randomUUID().toString();
        this.game = new Game();
        this.createdAt = Instant.now();
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.clock = (baseTimeSeconds > 0)
                ? new ChessClock(baseTimeSeconds * 1000L, incrementSeconds * 1000L)
                : ChessClock.untimed();
    }
}