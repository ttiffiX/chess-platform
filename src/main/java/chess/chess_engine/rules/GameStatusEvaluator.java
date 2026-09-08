package chess.chess_engine.rules;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.game.CastleRights;
import chess.chess_engine.model.game.GameResult;
import chess.chess_engine.model.game.GameStateSnapshot;
import chess.chess_engine.model.game.GameStatus;
import chess.chess_engine.model.Position;
import chess.chess_engine.model.piece.Color;
import chess.chess_engine.rules.services.DrawService;
import chess.chess_engine.rules.services.LegalMoveService;

import java.util.List;

public class GameStatusEvaluator {
    private final LegalMoveService legalMoveService;
    private final DrawService drawService;

    public GameStatusEvaluator(
            LegalMoveService legalMoveService,
            DrawService drawService) {
        if (legalMoveService == null || drawService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.legalMoveService = legalMoveService;
        this.drawService = drawService;
    }

    public GameResult evaluate(
            Board board,
            Color currentTurn,
            CastleRights castleRights,
            Position enPassTarget,
            int halfMoveClock,
            List<GameStateSnapshot> history,
            boolean inCheck
    ) {
        // 1. Kiểm tra các điều kiện hòa
        if (drawService.isFiftyMoveRule(halfMoveClock)) {
            return GameResult.draw(GameStatus.DRAW_FIFTY_MOVE_RULE);
        }

        if (drawService.isInsufficientMaterial(board)) {
            return GameResult.draw(GameStatus.DRAW_INSUFFICIENT_MATERIAL);
        }

        if (drawService.isThreefoldRepetition(history)) {
            return GameResult.draw(GameStatus.DRAW_THREEFOLD_REPETITION);
        }

        // 2. Kiểm tra nước đi hợp lệ
        boolean hasLegalMoves = legalMoveService.hasAnyLegalMove(board, currentTurn, castleRights, enPassTarget);

        if (!hasLegalMoves) {
            if (inCheck) {
                return GameResult.checkmate(currentTurn.opposite());
            }
            return GameResult.draw(GameStatus.STALEMATE);
        }

        return GameResult.inProgress();
    }
}