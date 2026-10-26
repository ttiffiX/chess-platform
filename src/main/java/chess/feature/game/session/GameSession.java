package chess.feature.game.session;

import chess.engine.model.game.Game;
import chess.engine.model.piece.Color;
import chess.feature.game.model.TimeControl;
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

    private Color drawOfferedBy;

    private final Instant createdAt;

    public GameSession(TimeControl timeControl, String whitePlayerId, String blackPlayerId) {
        this.gameId = UUID.randomUUID().toString();
        this.game = new Game();
        this.createdAt = Instant.now();
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.clock = new ChessClock(timeControl);
    }

    public void offerDraw(Color playerColor) {
        this.drawOfferedBy = playerColor;
    }

    public void clearDrawOffer() {
        this.drawOfferedBy = null;
    }

    public Color getPlayerColor(String playerId) {
        if (whitePlayerId.equals(playerId)) {
            return Color.WHITE;
        }

        if (blackPlayerId.equals(playerId)) {
            return Color.BLACK;
        }

        return null;
    }

    public void startClock(Instant now) {
        clock.start(game.getCurrentTurn(), now);
    }

    public void completeMove(Color moveColor, Instant moveReceivedAt, Instant nextTurnStartedAt) {
        clock.completeMove(moveColor, moveReceivedAt, nextTurnStartedAt);
    }

    public void stopClock(Instant now) {
        clock.stop(now);
    }

    public boolean checkTimeoutAndUpdate(Instant now) {
        if (game.isGameOver() || !clock.isTimedOut(now)) {
            return false;
        }

        clock.stop(now);
        game.isTimedOut(clock.getActiveColor());
        clearDrawOffer();
        return true;
    }

    public Long getTurnStartedAt() {
        return clock.getLastTimestamp().toEpochMilli();
    }

    public Long getWhiteRemainingMs() {
        return clock.getWhiteRemainingMs();
    }

    public Long getBlackRemainingMs() {
        return clock.getBlackRemainingMs();
    }
}