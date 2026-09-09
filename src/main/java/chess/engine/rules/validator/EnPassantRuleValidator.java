package chess.engine.rules.validator;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Pawn;
import chess.engine.model.piece.Piece;
import chess.engine.model.Position;
import chess.engine.rules.services.CheckService;
import chess.engine.rules.InvalidMoveReason;
import chess.engine.rules.ValidationContext;
import chess.engine.rules.ValidationResult;

import java.util.Objects;

public class EnPassantRuleValidator implements MoveRuleValidator {
    private final CheckService checkService;

    public EnPassantRuleValidator(CheckService checkService) {
        Objects.requireNonNull(checkService, "Check Service must not be null.");
        this.checkService = checkService;
    }

    @Override
    public boolean isApplicable(ValidationContext ctx) {
        Piece piece = ctx.board().getPiece(ctx.move().from());
        Position enPassTarget = ctx.enPassTarget();

        return piece instanceof Pawn
                && ctx.move().from().file() != ctx.move().to().file()
                && ctx.move().to().equals(enPassTarget);
    }

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        Board board = ctx.board();
        Move move = ctx.move();
        Color turn = ctx.turn();

        // 1. Kiểm tra cự ly: chỉ đi chéo đúng 1 ô theo hướng đi của màu đó
        int expectedRankStep = (turn == Color.WHITE) ? 1 : -1;
        int rankDiff = move.to().rank() - move.from().rank();
        int fileDiff = Math.abs(move.to().file() - move.from().file());

        if (rankDiff != expectedRankStep || fileDiff != 1) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_EN_PASSANT,
                    "Invalid en passant geometry."
            );
        }

        // 2. Tốt đối phương bị bắt nằm ngang hàng với quân Tốt đang đi
        Position capturedPawnPos = new Position(move.to().file(), move.from().rank());
        Piece capturedPiece = board.getPiece(capturedPawnPos);

        if (!(capturedPiece instanceof Pawn) || capturedPiece.getColor() == turn) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_EN_PASSANT,
                    "Target pawn to capture via en passant not found."
            );
        }

        // 3. Kiểm tra tự chiếu bằng cách mô phỏng di chuyển và xóa tạm con Tốt địch
        Board simulatedBoard = board.copy();
        simulatedBoard.removePiece(capturedPawnPos);

        if (checkService.leavesKingInCheck(simulatedBoard, move)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "En passant leaves your king in check."
            );
        }

        return ValidationResult.ok(b -> {
            b.movePiece(move);
            b.removePiece(capturedPawnPos);
        }, capturedPiece);
    }
}