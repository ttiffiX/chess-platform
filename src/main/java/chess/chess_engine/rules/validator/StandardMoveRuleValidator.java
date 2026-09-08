package chess.chess_engine.rules.validator;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.Move;
import chess.chess_engine.model.piece.Knight;
import chess.chess_engine.model.piece.Piece;
import chess.chess_engine.rules.InvalidMoveReason;
import chess.chess_engine.rules.ValidationContext;
import chess.chess_engine.rules.ValidationResult;
import chess.chess_engine.rules.services.CheckService;
import chess.chess_engine.rules.services.PathService;

public class StandardMoveRuleValidator implements MoveRuleValidator {
    private final PathService pathService;
    private final CheckService checkService;

    public StandardMoveRuleValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.pathService = pathService;
        this.checkService = checkService;
    }

    @Override
    public boolean isApplicable(ValidationContext ctx) {
        return true;
    }

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        Board board = ctx.board();
        Move move = ctx.move();

        Piece piece = board.getPiece(move.from());
        Piece targetPiece = board.getPiece(move.to());

        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return ValidationResult.fail(
                    InvalidMoveReason.TARGET_HAS_ALLY_PIECE,
                    "Cannot capture your own piece."
            );
        }

        boolean legalPattern = targetPiece == null
                ? piece.canMove(move.from(), move.to())
                : piece.canCapture(move.from(), move.to());

        if (!legalPattern) {
            return ValidationResult.fail(
                    InvalidMoveReason.ILLEGAL_PATTERN,
                    "Piece movement pattern is illegal."
            );
        }

        if (!(piece instanceof Knight) && !pathService.isPathClear(board, move.from(), move.to())) {
            return ValidationResult.fail(
                    InvalidMoveReason.PATH_BLOCKED,
                    "Path is blocked."
            );
        }

        if (checkService.leavesKingInCheck(board, move)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Move leaves your king in check."
            );
        }

        return ValidationResult.ok(b -> b.movePiece(move), targetPiece);
    }
}
