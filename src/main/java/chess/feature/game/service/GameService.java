package chess.feature.game.service;

import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.dto.response.MoveHistoryDto;
import chess.feature.game.dto.response.PieceDto;
import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.piece.Color;
import chess.engine.rules.ValidationResult;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.model.TimeControl;
import chess.feature.game.session.GameSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    public GameSession createGame(CreateGameRequest request) {
        if (request.baseTimeSeconds() == 0 && request.incrementSeconds() != 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Base time cannot be zero if increment is non-zero.");
        }

        TimeControl timeControl = new TimeControl(request.baseTimeSeconds(), request.incrementSeconds());

        boolean player1IsWhite = ThreadLocalRandom.current().nextBoolean();
        String whiteId = player1IsWhite ? request.player1Id() : request.player2Id();
        String blackId = player1IsWhite ? request.player2Id() : request.player1Id();

        GameSession session = new GameSession(timeControl, whiteId, blackId);

        session.startClock(Instant.now());
        activeGames.put(session.getGameId(), session);
        return session;
    }

    public GameStateResponse getGame(String gameId, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            resolvePlayerColor(session, currentPlayerId);

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse makeMove(String gameId, MoveRequest request, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Game game = session.getGame();
            Color expectedTurnColor = resolveIsPlayerTurn(session, currentPlayerId, game.getCurrentTurn());

            Instant moveReceivedAt = Instant.now();

            if (session.checkTimeoutAndUpdate(moveReceivedAt)) {
                return toGameStateResponse(session);
            }


            Move move = request.toMove(expectedTurnColor);

            // Gọi Engine trả về kết quả
            ValidationResult result = game.move(move);
            if (!result.valid()) {
                throw new AppException(ErrorCode.INVALID_MOVE, result.message());
            }

            Instant nextTurnStartedAt = Instant.now();

            session.completeMove(expectedTurnColor, moveReceivedAt, nextTurnStartedAt);

            if (game.isGameOver()) {
                session.stopClock(nextTurnStartedAt);
                session.clearDrawOffer();
            }

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse resign(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();
            if (session.checkTimeoutAndUpdate(now)) {
                return toGameStateResponse(session);
            }

            Game game = session.getGame();

            boolean success = game.resign(playerColor);
            if (!success) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            session.stopClock(now);
            session.clearDrawOffer();

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse offerDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);
            Instant now = Instant.now();

            if (session.checkTimeoutAndUpdate(now)) {
                return toGameStateResponse(session);
            }

            Game game = session.getGame();

            if (game.isGameOver()) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
            }

            if (session.getDrawOfferedBy() != null) {
                throw new AppException(ErrorCode.INVALID_MOVE, "A draw offer is already pending");
            }

            session.offerDraw(playerColor);

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse acceptDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();

            if (session.checkTimeoutAndUpdate(now)) {
                return toGameStateResponse(session);
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

            session.stopClock(now);
            session.clearDrawOffer();

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse declineDraw(String gameId, String currentUserId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            Color playerColor = resolvePlayerColor(session, currentUserId);

            Instant now = Instant.now();

            if (session.checkTimeoutAndUpdate(now)) {
                return toGameStateResponse(session);
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

            return toGameStateResponse(session);
        }
    }

    public GameStateResponse getGameSnapshotAt(String gameId, String currentUserId, int moveIndex) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            resolvePlayerColor(session, currentUserId);
            Game game = session.getGame();

            if (moveIndex < 0 || moveIndex >= game.getGameStateHistory().size()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Invalid move index: " + moveIndex);
            }

            GameStateSnapshot snapshot = game.getSnapshotAt(moveIndex);
            return toSnapshotResponse(session, snapshot, game.getGameStateHistory());
        }
    }

    public GameStateResponse claimTimeout(String gameId, String currentPlayerId) {
        GameSession session = getSession(gameId);

        synchronized (session) {
            resolvePlayerColor(session, currentPlayerId);

            Instant now = Instant.now();

            session.checkTimeoutAndUpdate(now);

            return toGameStateResponse(session);
        }
    }


    // Helper methods
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

    private GameStateResponse toSnapshotResponse(GameSession session, GameStateSnapshot
            snapshot, List<GameStateSnapshot> history) {
        Map<String, PieceDto> boardMap = new HashMap<>();
        snapshot.board().getPieces().forEach((pos, piece) -> {
            if (piece != null) {
                boardMap.put(pos.toString(), new PieceDto(piece));
            }
        });

        List<MoveHistoryDto> historyItems = getMoveHistoryList(history);

        return new GameStateResponse(
                session.getGameId(),
                snapshot.gameResult(),
                snapshot.turn(),
                snapshot.inCheck(),
                snapshot.fullMoveNumber(),
                boardMap,
                List.of(),
                List.of(),
                historyItems,
                Map.of(),  // Không có legal moves vì đang ở chế độ xem lại
                session.getCreatedAt(),
                null,
                null,
                null,
                session.getWhitePlayerId(),
                session.getBlackPlayerId(),
                session.getDrawOfferedBy()
        );
    }

    private GameStateResponse toGameStateResponse(GameSession session) {
        Game game = session.getGame();
        Board board = game.getBoard();

        // 1. Ánh xạ Board -> Map<String, PieceDto> ("e4" -> PieceDto)
        Map<String, PieceDto> boardMap = new HashMap<>();
        board.getPieces().forEach((pos, piece) -> {
            if (piece != null) {
                boardMap.put(pos.toString(), new PieceDto(piece));
            }
        });

        // 2. Quân cờ bị bắt theo từng màu
        List<PieceDto> capturedWhite = game.getCapturedPiecesByColor(Color.WHITE).stream().map(PieceDto::new).toList();

        List<PieceDto> capturedBlack = game.getCapturedPiecesByColor(Color.BLACK).stream().map(PieceDto::new).toList();

        // 3. Tóm tắt danh sách nước đi cho FE click xem lại
        List<MoveHistoryDto> historyItems = getMoveHistoryList(game.getGameStateHistory());

        Long turnStartEpochMs = session.getTurnStartedAt();

        return new GameStateResponse(
                session.getGameId(),
                game.getGameResult(),
                game.getCurrentTurn(),
                game.isInCheck(),
                game.getFullMoveNumber(),
                boardMap,
                capturedWhite,
                capturedBlack,
                historyItems,
                extractLegalMovesMap(game),
                session.getCreatedAt(),
                session.getWhiteRemainingMs(),
                session.getBlackRemainingMs(),
                turnStartEpochMs,
                session.getWhitePlayerId(),
                session.getBlackPlayerId(),
                session.getDrawOfferedBy()
        );
    }

    private List<MoveHistoryDto> getMoveHistoryList(List<GameStateSnapshot> history) {
        List<MoveHistoryDto> historyItems = new ArrayList<>();

        for (int i = 1; i < history.size(); i++) {
            Move m = history.get(i).lastMove();
            if (m != null) {
                historyItems.add(new MoveHistoryDto(i, m.from().toString(), m.to().toString()));
            }
        }
        return historyItems;
    }

    private Map<String, List<String>> extractLegalMovesMap(Game game) {
        Map<String, List<String>> legalMovesMap = new HashMap<>();
        List<Move> legalMoves = game.getAllLegalMovesByColor();

        for (Move move : legalMoves) {
            legalMovesMap.computeIfAbsent(move.from().toString(), _ -> new ArrayList<>()).add(move.to().toString());
        }
        return legalMovesMap;
    }
}
