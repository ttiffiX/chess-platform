package rules;

import model.Board;
import model.Move;
import piece.Color;
import piece.King;
import piece.Piece;
import model.Position;

public class CastlingRuleValidator implements MoveRuleValidator {
    private final PathService pathService;
    private final CheckService checkService;

    public CastlingRuleValidator(PathService pathService, CheckService checkService) {
        if (pathService == null || checkService == null) {
            throw new IllegalArgumentException("Services must not be null.");
        }
        this.pathService = pathService;
        this.checkService = checkService;
    }

    @Override
    public boolean isApplicable(ValidationContext ctx) {
        Piece piece = ctx.board().getPiece(ctx.move().from());
        return piece instanceof King
                && Math.abs(ctx.move().from().file() - ctx.move().to().file()) == 2;
    }

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        Board board = ctx.board();
        Move move = ctx.move();
        Color turn = ctx.turn();

        // 1. Phải di chuyển ngang trên cùng một hàng
        if (move.from().rank() != move.to().rank()) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_CASTLING,
                    "Castling must be on the same rank."
            );
        }

        boolean isKingSide = move.to().file() > move.from().file();
        int rank = move.from().rank();

        // 2. Kiểm tra quyền nhập thành
        boolean hasRight = isKingSide
                ? ctx.castleRights().canCastleKingSide(turn)
                : ctx.castleRights().canCastleQueenSide(turn);

        if (!hasRight) {
            return ValidationResult.fail(
                    InvalidMoveReason.INVALID_CASTLING,
                    "Castling rights already lost."
            );
        }

        // 3. Kiểm tra đường đi giữa Vua và Xe tương ứng
        Position rookPos = isKingSide ? new Position('h', rank) : new Position('a', rank);
        if (!pathService.isPathClear(board, move.from(), rookPos)) {
            return ValidationResult.fail(
                    InvalidMoveReason.PATH_BLOCKED,
                    "Pieces are blocking the castling path."
            );
        }

        // 4. Vua không bị chiếu ở vị trí hiện tại, ô đi qua, và ô đích
        Color opponent = turn.opposite();
        int step = isKingSide ? 1 : -1;
        Position transitSquare = new Position((char) (move.from().file() + step), rank);

        if (checkService.isSquareUnderAttack(board, move.from(), opponent)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Cannot castle while in check."
            );
        }

        if (checkService.isSquareUnderAttack(board, transitSquare, opponent)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Cannot castle through an attacked square."
            );
        }

        if (checkService.isSquareUnderAttack(board, move.to(), opponent)) {
            return ValidationResult.fail(
                    InvalidMoveReason.KING_LEFT_IN_CHECK,
                    "Cannot castle into check."
            );
        }

        Position rookTo = isKingSide ? new Position('f', rank) : new Position('d', rank);

        return ValidationResult.ok(b -> {
                    b.movePiece(move);
                    b.movePiece(new Move(rookPos, rookTo));
                },
                null
        );
    }
}