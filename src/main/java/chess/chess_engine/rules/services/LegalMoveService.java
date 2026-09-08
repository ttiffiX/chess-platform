package chess.chess_engine.rules.services;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.Move;
import chess.chess_engine.model.Position;
import chess.chess_engine.model.game.CastleRights;
import chess.chess_engine.model.piece.Color;
import chess.chess_engine.model.piece.Piece;
import chess.chess_engine.rules.ValidationContext;
import chess.chess_engine.rules.validator.MoveValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LegalMoveService {
    private final MoveValidator moveValidator;

    public LegalMoveService(MoveValidator moveValidator) {
        if (moveValidator == null) {
            throw new IllegalArgumentException("MoveValidator must not be null.");
        }
        this.moveValidator = moveValidator;
    }

    public List<Move> generateLegalMoves(Board board, Color color, CastleRights castleRights, Position enPassTarget) {
        List<Move> legalMoves = new ArrayList<>();
        Map<Position, Piece> pieces = board.getPiecesByColor(color);

        for (Position from : pieces.keySet()) {
            for (char file = 'a'; file <= 'h'; file++) {
                for (int rank = 1; rank <= 8; rank++) {
                    Position to = new Position(file, rank);
                    if (from.equals(to)) continue;

                    Move move = new Move(from, to);
                    ValidationContext ctx = new ValidationContext(board, move, color, castleRights, enPassTarget);

                    if (moveValidator.validate(ctx).valid()) {
                        legalMoves.add(move);
                    }
                }
            }
        }
        return legalMoves;
    }

    public boolean hasAnyLegalMove(Board board, Color color, CastleRights castleRights, Position enPassTarget) {
        Map<Position, Piece> pieces = board.getPiecesByColor(color);

        for (Position from : pieces.keySet()) {
            for (char file = 'a'; file <= 'h'; file++) {
                for (int rank = 1; rank <= 8; rank++) {
                    Position to = new Position(file, rank);
                    if (from.equals(to)) continue;

                    Move move = new Move(from, to);
                    ValidationContext ctx = new ValidationContext(board, move, color, castleRights, enPassTarget);

                    if (moveValidator.validate(ctx).valid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
