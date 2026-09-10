package chess.session;

import chess.engine.model.game.Game;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class GameSession {
    private final String gameId;
    private final Game game;
    private final Instant createdAt;

    public GameSession() {
        this.gameId = UUID.randomUUID().toString();
        this.game = new Game();
        this.createdAt = Instant.now();
    }
}