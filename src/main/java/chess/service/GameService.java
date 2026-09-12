package chess.service;

import chess.dto.request.CreateGameRequest;
import chess.dto.request.MoveRequest;
import chess.dto.response.GameStateResponse;
import chess.dto.response.MoveHistoryDto;
import chess.dto.response.PieceDto;
import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.engine.rules.ValidationResult;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.session.ChessClock;
import chess.session.GameSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    public GameStateResponse createGame(CreateGameRequest request) {
        long base = (request.baseTimeSeconds() != null) ? request.baseTimeSeconds() : 0L;
        long inc = (request.incrementSeconds() != null) ? request.incrementSeconds() : 0L;

        GameSession session = new GameSession(base, inc);
        activeGames.put(session.getGameId(), session);
        return toGameStateResponse(session);
    }

    public GameStateResponse getGame(String gameId) {
        GameSession session = getSession(gameId);
        checkTimeoutAndUpdate(session);
        return toGameStateResponse(session);
    }

    public GameStateResponse makeMove(String gameId, MoveRequest request) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();
        ChessClock clock = session.getClock();
        Instant now = Instant.now();

        if (clock.isTimedOut(game.getCurrentTurn(), now)) {
            handleTimeout(game, clock, game.getCurrentTurn());
            throw new AppException(ErrorCode.INVALID_MOVE, "Time out! Turn has expired.");
        }

        Move move = request.toMove(game.getCurrentTurn());
        Color playerColor = game.getCurrentTurn();

        // Gọi Engine trả về kết quả
        ValidationResult result = game.move(move);
        if (!result.valid()) {
            throw new AppException(ErrorCode.INVALID_MOVE, result.message());
        }

        clock.onMove(playerColor, now);

        return toGameStateResponse(session);
    }

    public GameStateResponse undoMove(String gameId, Color playerColor) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        // Nếu không truyền color (chơi 1 máy chung nút Undo), mặc định lấy lượt hiện tại
        Color requester = (playerColor != null) ? playerColor : game.getCurrentTurn();

        int historySize = game.getGameStateHistory().size();
        int totalMovesPlayed = historySize - 1;

        if (totalMovesPlayed == 0) {
            throw new AppException(ErrorCode.INVALID_MOVE, "No moves to undo");
        }

        int stepsToUndo;
        if (requester == game.getCurrentTurn()) {
            // Đang là lượt mình mà muốn undo nước của mình thì phải lùi cả nước vừa rồi của đối phương
            // Cần ít nhất 2 nước đã đi trong ván cờ
            if (totalMovesPlayed < 2) {
                throw new AppException(ErrorCode.INVALID_MOVE, "Cannot undo: No previous move of yours to revert to");
            }
            stepsToUndo = 2;
        } else {
            // Vừa đi xong đối phương chưa đi: lùi 1 nước
            stepsToUndo = 1;
        }

        // Thực hiện lùi đồng thời
        boolean success = game.undo(stepsToUndo);
        if (!success) {
            throw new AppException(ErrorCode.INVALID_MOVE, "Failed to undo moves");
        }
        session.getClock().undo(stepsToUndo);

        return toGameStateResponse(session);
    }
    
    public Map<String, List<String>> getLegalMoves(String gameId) {
        GameSession session = getSession(gameId);
        return extractLegalMovesMap(session.getGame());
    }

    public GameStateResponse resign(String gameId, Color playerColor) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        boolean success = game.resign(playerColor);
        if (!success) {
            throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
        }
        session.getClock().stop();

        return toGameStateResponse(session);
    }

    public GameStateResponse offerDraw(String gameId) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        boolean success = game.agreeDraw();
        if (!success) {
            throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
        }
        session.getClock().stop();
        return toGameStateResponse(session);
    }

    public GameStateResponse claimTimeout(String gameId) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();
        ChessClock clock = session.getClock();
        Instant now = Instant.now();

        if (game.getGameResult().status() != GameStatus.IN_PROGRESS) {
            return toGameStateResponse(session);
        }

        if (clock.isTimedOut(game.getCurrentTurn(), now)) {
            handleTimeout(game, clock, game.getCurrentTurn());
        }

        return toGameStateResponse(session);
    }

    public GameStateResponse getGameSnapshotAt(String gameId, int moveIndex) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        if (moveIndex < 0 || moveIndex >= game.getGameStateHistory().size()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Invalid move index: " + moveIndex);
        }

        GameStateSnapshot snapshot = game.getSnapshotAt(moveIndex);
        return toSnapshotResponse(session, snapshot, game.getGameStateHistory());
    }


    // Helper methods
    private GameStateResponse toSnapshotResponse(GameSession session, GameStateSnapshot snapshot, List<GameStateSnapshot> history) {
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
                null
        );
    }

    private void checkTimeoutAndUpdate(GameSession session) {
        Game game = session.getGame();
        ChessClock clock = session.getClock();
        if (game.getGameResult().status() == GameStatus.IN_PROGRESS && clock.isTimedOut(game.getCurrentTurn(), Instant.now())) {
            handleTimeout(game, clock, game.getCurrentTurn());
        }
    }

    private void handleTimeout(Game game, ChessClock clock, Color timedOutColor) {
        clock.stop();
        // Bên hết giờ sẽ bị xử thua
        game.resign(timedOutColor);
    }

    private GameSession getSession(String gameId) {
        GameSession session = activeGames.get(gameId);
        if (session == null) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND, "Room not found with ID: " + gameId);
        }
        return session;
    }

    private GameStateResponse toGameStateResponse(GameSession session) {
        Game game = session.getGame();
        Board board = game.getBoard();
        ChessClock clock = session.getClock();

        // 1. Ánh xạ Board -> Map<String, PieceDto> ("e4" -> PieceDto)
        Map<String, PieceDto> boardMap = new HashMap<>();
        board.getPieces().forEach((pos, piece) -> {
            if (piece != null) {
                boardMap.put(pos.toString(), new PieceDto(piece));
            }
        });

        // 2. Quân cờ bị bắt theo từng màu
        List<PieceDto> capturedWhite = game.getCapturedPiecesByColor(Color.WHITE).stream()
                .map(PieceDto::new)
                .toList();

        List<PieceDto> capturedBlack = game.getCapturedPiecesByColor(Color.BLACK).stream()
                .map(PieceDto::new)
                .toList();

        // 3. Tóm tắt danh sách nước đi cho FE click xem lại
        List<GameStateSnapshot> history = game.getGameStateHistory();
        List<MoveHistoryDto> historyItems = getMoveHistoryList(history);

        Long turnStartEpochMs = (clock.getLastMoveTimestamp() != null)
                ? clock.getLastMoveTimestamp().toEpochMilli()
                : null;

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
                clock.getWhiteRemainingMs(),
                clock.getBlackRemainingMs(),
                turnStartEpochMs
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
            legalMovesMap
                    .computeIfAbsent(move.from().toString(), _ -> new ArrayList<>())
                    .add(move.to().toString());
        }
        return legalMovesMap;
    }
}
