package chess.feature.game.service;

import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameEventResponse;
import chess.feature.game.dto.response.GameResponse;
import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.piece.Color;
import chess.engine.rules.ValidationResult;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.dto.response.GameUpdateResponse;
import chess.feature.game.model.TimeControl;
import chess.feature.game.session.GameSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    private final Object playerGameLock = new Object();

    private final Map<String, String> activeGameByPlayer = new HashMap<>();

    public GameSession createGame(CreateGameRequest request) {
        validateCreateGameRequest(request);

        String player1Id = request.player1Id();
        String player2Id = request.player2Id();

        TimeControl timeControl = new TimeControl(request.baseTimeSeconds(), request.incrementSeconds());

        boolean player1IsWhite = ThreadLocalRandom.current().nextBoolean();
        String whiteId = player1IsWhite ? player1Id : player2Id;
        String blackId = player1IsWhite ? player2Id : player1Id;

        GameSession session = new GameSession(timeControl, whiteId, blackId);

        synchronized (playerGameLock) {

            if (activeGameByPlayer.containsKey(player1Id) || activeGameByPlayer.containsKey(player2Id)) {
                throw new AppException(ErrorCode.ALREADY_IN_GAME, "One or both players are already in an active game.");
            }

            String gameId = session.getGameId();

            session.startClock(Instant.now());

            activeGames.put(gameId, session);

            activeGameByPlayer.put(player1Id, gameId);
            activeGameByPlayer.put(player2Id, gameId);
        }
        return session;
    }

    public GameResponse getGame(String gameId, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            resolvePlayerColor(session, currentPlayerId);

            return toGameResponse(session);
        }
    }

    public GameUpdateResponse makeMove(String gameId, MoveRequest request, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Game game = session.getGame();
            Color expectedTurnColor = resolveIsPlayerTurn(session, currentPlayerId, game.getCurrentTurn());

            Instant moveReceivedAt = Instant.now();

            if (checkTimeoutAndUpdate(session, moveReceivedAt)) {
                return toGameUpdateResponse(session);
            }

            Move move = request.toMove(expectedTurnColor);

            // Gọi Engine trả về kết quả
            ValidationResult result = game.move(move);
            if (!result.valid()) {
                throw new AppException(ErrorCode.INVALID_MOVE, result.message());
            }

            Instant nextTurnStartedAt = Instant.now();

            session.completeMove(expectedTurnColor, moveReceivedAt, nextTurnStartedAt);

            session.addHistory(move.toUCI());

            if (session.finalizeGameIfOver(nextTurnStartedAt)) {
                releasePlayers(session);
            }

            return toGameUpdateResponse(session);
        }
    }

    public GameEventResponse resign(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();
            if (checkTimeoutAndUpdate(session, now)) {
                return toGameEventResponse(session);
            }

            Game game = session.getGame();

            boolean success = game.resign(playerColor);
            if (!success) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            if (session.finalizeGameIfOver(now)) {
                releasePlayers(session);
            }
            return toGameEventResponse(session);
        }
    }

    public GameEventResponse offerDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);
            Instant now = Instant.now();


            if (checkTimeoutAndUpdate(session, now)) {
                return toGameEventResponse(session);
            }

            Game game = session.getGame();

            if (game.isGameOver()) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            if (session.getDrawOfferedBy() != null) {
                throw new AppException(ErrorCode.INVALID_MOVE, "A draw offer is already pending");
            }

            session.offerDraw(playerColor);

            return toGameEventResponse(session);
        }
    }

    public GameEventResponse acceptDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();
            if (checkTimeoutAndUpdate(session, now)) {
                return toGameEventResponse(session);
            }

            Game game = session.getGame();

            Color drawOfferedBy = session.getDrawOfferedBy();

            if (game.isGameOver()) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            if (drawOfferedBy == null) {
                throw new AppException(ErrorCode.INVALID_MOVE, "There is no pending draw offer");
            }

            if (playerColor == drawOfferedBy) {
                throw new AppException(ErrorCode.INVALID_MOVE, "You cannot accept your own draw offer");
            }

            boolean success = game.agreeDraw();

            if (!success) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            if (session.finalizeGameIfOver(now)) {
                releasePlayers(session);
            }
            return toGameEventResponse(session);
        }
    }

    public GameEventResponse declineDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();

            if (checkTimeoutAndUpdate(session, now)) {
                return toGameEventResponse(session);
            }

            Color drawOfferedBy = session.getDrawOfferedBy();

            if (session.getGame().isGameOver()) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }


            if (drawOfferedBy == null) {
                throw new AppException(ErrorCode.INVALID_MOVE, "There is no pending draw offer");
            }

            if (playerColor == drawOfferedBy) {
                throw new AppException(ErrorCode.INVALID_MOVE, "You cannot decline your own draw offer");
            }

            session.clearDrawOffer();

            return toGameEventResponse(session);
        }
    }


    public GameEventResponse claimTimeout(String gameId, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            resolvePlayerColor(session, currentPlayerId);

            Instant now = Instant.now();

            checkTimeoutAndUpdate(session, now);

            return toGameEventResponse(session);
        }
    }

    public boolean hasActiveGame(String playerId) {
        synchronized (playerGameLock) {
            return activeGameByPlayer.containsKey(playerId);
        }
    }

    public String getActiveGameId(String playerId) {
        synchronized (playerGameLock) {
            return activeGameByPlayer.get(playerId);
        }
    }

    public boolean isPlayerInGame(String gameId, String userId) {
        if (gameId == null || gameId.isBlank()
                || userId == null || userId.isBlank()) {
            return false;
        }

        GameSession session = activeGames.get(gameId);

        return session != null && session.getPlayerColor(userId) != null;
    }

    // Helper methods
    private void validateCreateGameRequest(CreateGameRequest request) {
        if (request == null) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Create game request must not be null."
            );
        }

        String player1Id = request.player1Id();
        String player2Id = request.player2Id();

        if (player1Id == null || player1Id.isBlank()
                || player2Id == null || player2Id.isBlank()) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Player IDs must not be blank."
            );
        }

        if (player1Id.equals(player2Id)) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Players must be different."
            );
        }

        Long baseTime = request.baseTimeSeconds();
        Long increment = request.incrementSeconds();

        if (baseTime == null || increment == null
                || baseTime < 0 || baseTime > 10800
                || increment < 0 || increment > 180
                || (baseTime == 0 && increment != 0)) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Invalid time control."
            );
        }
    }

    private Color resolveIsPlayerTurn(GameSession session, String userId, Color expectedColor) {
        Color color = session.getPlayerColor(userId);
        if (color == null) {
            throw new AppException(ErrorCode.PLAYER_NOT_IN_GAME, "User is not a participant of this game.");
        }
        if (color != expectedColor) {
            throw new AppException(ErrorCode.INVALID_MOVE, "It's not your turn.");
        }
        return expectedColor;
    }

    private Color resolvePlayerColor(GameSession session, String userId) {
        Color color = session.getPlayerColor(userId);
        if (color == null) {
            throw new AppException(ErrorCode.PLAYER_NOT_IN_GAME, "User is not a participant of this game.");
        }

        return color;
    }

    private GameSession getSession(String gameId) {
        GameSession session = activeGames.get(gameId);
        if (session == null) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND, "Room not found with ID: " + gameId);
        }
        return session;
    }

    private GameEventResponse toGameEventResponse(GameSession session) {
        return new GameEventResponse(
                session.getGameId(),
                session.getGame().getGameResult(),
                session.getDrawOfferedBy(),
                session.getStateVersion()
        );
    }

    private GameResponse toGameResponse(GameSession session) {
        return new GameResponse(
                toGameUpdateResponse(session),
                session.getWhitePlayerId(),
                session.getBlackPlayerId(),
                session.getDrawOfferedBy(),
                session.getCreatedAt()
        );
    }

    private GameUpdateResponse toGameUpdateResponse(GameSession session) {
        return new GameUpdateResponse(
                session.getGameId(),
                session.getGame().getGameResult(),
                session.getGame().isInCheck(),
                session.getFEN(),
                session.getLastHistory(),
                session.extractLegalMovesMap(),
                session.getWhiteRemainingMs(),
                session.getBlackRemainingMs(),
                session.getTurnStartedAt(),
                Instant.now().toEpochMilli(),
                session.getStateVersion()
        );
    }

    private void releasePlayers(GameSession session) {
        synchronized (playerGameLock) {
            String gameId = session.getGameId();

            activeGameByPlayer.remove(session.getWhitePlayerId(), gameId);
            activeGameByPlayer.remove(session.getBlackPlayerId(), gameId);
        }
    }

    private boolean checkTimeoutAndUpdate(GameSession session, Instant now) {
        session.incrementStateVersion();

        if (!session.checkTimeoutAndUpdate(now)) {
            return false;
        }

        releasePlayers(session);
        return true;
    }
}
