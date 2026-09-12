package chess.session;

import chess.engine.model.game.Game;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class GameSession {
    private final String gameId;
    private final Game game;
    private final ChessClock clock;
    private final Instant createdAt;

    public GameSession(long baseTimeSeconds, long incrementSeconds) {
        this.gameId = UUID.randomUUID().toString();
        this.game = new Game();
        this.createdAt = Instant.now();
        this.clock = (baseTimeSeconds > 0)
                ? new ChessClock(baseTimeSeconds * 1000L, incrementSeconds * 1000L)
                : ChessClock.untimed();
    }

    public GameSession() {
        this(0, 0); // Default to untimed game
    }
}