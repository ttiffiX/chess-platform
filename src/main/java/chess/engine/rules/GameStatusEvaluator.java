package chess.engine.rules;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.game.GameResult;
import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.engine.rules.services.DrawService;

import java.util.List;
import java.util.Objects;

public class GameStatusEvaluator {
    private final DrawService drawService;

    public GameStatusEvaluator(DrawService drawService) {
        this.drawService = Objects.requireNonNull(drawService, "Draw Service must not be null.");
    }

    public GameResult evaluate(
            Board board,
            Color currentTurn,
            int halfMoveClock,
            int positionOccurrenceCount,
            boolean inCheck,
            List<Move> legalMoves
    ) {
        Objects.requireNonNull(board, "Board must not be null.");
        Objects.requireNonNull(currentTurn, "Current turn must not be null.");
        Objects.requireNonNull(legalMoves, "Legal moves must not be null.");

        if (legalMoves.isEmpty()) {
            if (inCheck) {
                return GameResult.checkmate(currentTurn.opposite());
            }
            return GameResult.draw(GameStatus.STALEMATE);
        }

        if (drawService.isFiftyMoveRule(halfMoveClock)) {
            return GameResult.draw(GameStatus.DRAW_FIFTY_MOVE_RULE);
        }

        if (drawService.isInsufficientMaterial(board)) {
            return GameResult.draw(GameStatus.DRAW_INSUFFICIENT_MATERIAL);
        }

        if (drawService.isThreefoldRepetition(positionOccurrenceCount)) {
            return GameResult.draw(GameStatus.DRAW_THREEFOLD_REPETITION);
        }

        return GameResult.inProgress();
    }
}