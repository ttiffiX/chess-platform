package chess.service;

import chess.dto.request.MoveRequest;
import chess.dto.response.GameStateResponse;
import chess.dto.response.MoveHistoryDto;
import chess.dto.response.PieceDto;
import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameStateSnapshot;
import chess.engine.model.piece.Color;
import chess.engine.rules.ValidationResult;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.session.GameSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    public GameStateResponse createGame() {
        GameSession session = new GameSession();
        activeGames.put(session.getGameId(), session);
        return toGameStateResponse(session);
    }

    public GameStateResponse getGame(String gameId) {
        GameSession session = getSession(gameId);
        return toGameStateResponse(session);
    }

    public GameStateResponse makeMove(String gameId, MoveRequest request) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        Move move = request.toMove(game.getCurrentTurn());

        // Gọi Engine trả về kết quả
        ValidationResult result = game.move(move);
        if (!result.valid()) {
            throw new AppException(ErrorCode.INVALID_MOVE, result.message());
        }

        return toGameStateResponse(session);
    }

    public GameStateResponse undoMove(String gameId) {
        GameSession session = getSession(gameId);
        boolean success = session.getGame().undo();

        if (!success) {
            throw new AppException(ErrorCode.INVALID_MOVE, "No moves to undo");
        }
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

        return toGameStateResponse(session);
    }

    public GameStateResponse offerDraw(String gameId) {
        GameSession session = getSession(gameId);
        Game game = session.getGame();

        boolean success = game.agreeDraw();
        if (!success) {
            throw new AppException(ErrorCode.INVALID_MOVE, "Game is already finished");
        }

        return toGameStateResponse(session);
    }


    // Helper methods

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
        List<MoveHistoryDto> historyItems = new ArrayList<>();

        for (int i = 1; i < history.size(); i++) {
            Move m = history.get(i).lastMove();
            if (m != null) {
                historyItems.add(new MoveHistoryDto(i, m.from().toString(), m.to().toString()));
            }
        }

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
                session.getCreatedAt()
        );
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
