package chess.chess_engine.rules.services;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.Move;
import chess.chess_engine.model.piece.Color;
import chess.chess_engine.model.piece.Knight;
import chess.chess_engine.model.piece.Piece;
import chess.chess_engine.model.Position;

import java.util.Map;

public class CheckService {
    private final PathService pathService;

    public CheckService(PathService pathService) {
        if (pathService == null) {
            throw new IllegalArgumentException("PathService must not be null.");
        }
        this.pathService = pathService;
    }

    public boolean isInCheck(Board board, Color color) {
        if (board == null || color == null) {
            throw new IllegalArgumentException("Board and color must not be null.");
        }

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
        if (board == null || move == null) {
            throw new IllegalArgumentException("Board and move must not be null.");
        }

        Piece movingPiece = board.getPiece(move.from());
        if (movingPiece == null) {
            return true;
        }

        Board hypotheticalBoard = board.copy();
        hypotheticalBoard.movePiece(move);
        return isInCheck(hypotheticalBoard, movingPiece.getColor());
    }

    public boolean isSquareUnderAttack(Board board, Position position, Color color) {
        if (board == null || position == null || color == null) {
            throw new IllegalArgumentException("Board, position and color must not be null.");
        }

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
