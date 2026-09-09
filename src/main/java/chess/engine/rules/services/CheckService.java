package chess.engine.rules.services;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Knight;
import chess.engine.model.piece.Piece;
import chess.engine.model.Position;

import java.util.Map;
import java.util.Objects;

public class CheckService {
    private final PathService pathService;

    public CheckService(PathService pathService) {
        Objects.requireNonNull(pathService, "PathService must not be null");
        this.pathService = pathService;
    }

    public boolean isInCheck(Board board, Color color) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(color, "Color must not be null");

        Position kingPosition = board.findKing(color);
        Map<Position, Piece> pieces = board.getPieces();

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position from = entry.getKey();
            Piece enemyPiece = entry.getValue();

            if (enemyPiece.getColor() == color) {
                continue;
            }

            if (!enemyPiece.canCapture(from, kingPosition)) {
                continue;
            }

            if (enemyPiece instanceof Knight || pathService.isPathClear(board, from, kingPosition)) {
                return true;
            }
        }
        return false;
    }

    public boolean leavesKingInCheck(Board board, Move move) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(move, "Move must not be null");

        Piece movingPiece = board.getPiece(move.from());
        if (movingPiece == null) {
            return true;
        }

        Board hypotheticalBoard = board.copy();
        hypotheticalBoard.movePiece(move);
        return isInCheck(hypotheticalBoard, movingPiece.getColor());
    }

    public boolean isSquareUnderAttack(Board board, Position position, Color color) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(position, "Position must not be null");
        Objects.requireNonNull(color, "Color must not be null");

        Map<Position, Piece> pieces = board.getPieces();

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position from = entry.getKey();
            Piece enemyPiece = entry.getValue();

            if (enemyPiece.getColor() != color) {
                continue;
            }

            if (!enemyPiece.canCapture(from, position)) {
                continue;
            }

            if (enemyPiece instanceof Knight || pathService.isPathClear(board, from, position)) {
                return true;
            }
        }
        return false;
    }
}
