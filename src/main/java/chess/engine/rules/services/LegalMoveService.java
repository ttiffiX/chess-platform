package chess.engine.rules.services;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.CastleRights;
import chess.engine.model.piece.*;
import chess.engine.rules.ValidationContext;
import chess.engine.rules.validator.MoveValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LegalMoveService {
    private final MoveValidator moveValidator;

    public LegalMoveService(MoveValidator moveValidator) {
        this.moveValidator = Objects.requireNonNull(
                moveValidator, "Move Validator must not be null."
        );
    }

    public List<Move> generateLegalMoves(
            Board board,
            Color color,
            CastleRights castleRights,
            Position enPassTarget
    ) {
        Objects.requireNonNull(board, "Board must not be null.");
        Objects.requireNonNull(color, "Color must not be null.");

        List<Move> legalMoves = new ArrayList<>();
        Map<Position, Piece> pieces = board.getPiecesByColor(color);

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position from = entry.getKey();
            Piece piece = entry.getValue();

            for (Position to : piece.getCandidateDestinations(from)) {
                for (Move move : createCandidateMoves(piece, from, to)) {
                    if (isLegal(board, move, color, castleRights, enPassTarget)) {
                        legalMoves.add(move);
                    }
                }
            }
        }

        return legalMoves;
    }

    public boolean hasAnyLegalMove(
            Board board,
            Color color,
            CastleRights castleRights,
            Position enPassTarget
    ) {
        Objects.requireNonNull(board, "Board must not be null.");
        Objects.requireNonNull(color, "Color must not be null.");

        Map<Position, Piece> pieces = board.getPiecesByColor(color);

        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Position from = entry.getKey();
            Piece piece = entry.getValue();

            for (Position to : piece.getCandidateDestinations(from)) {
                for (Move move : createCandidateMoves(piece, from, to)) {
                    if (isLegal(board, move, color, castleRights, enPassTarget)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<Move> createCandidateMoves(Piece piece, Position from, Position to) {
        if (piece instanceof Pawn && to.rank() == (piece.getColor() == Color.WHITE ? 8 : 1)) {
            Color color = piece.getColor();

            return List.of(
                    new Move(from, to, new Queen(color)),
                    new Move(from, to, new Rook(color)),
                    new Move(from, to, new Bishop(color)),
                    new Move(from, to, new Knight(color))
            );
        }

        return List.of(new Move(from, to));
    }

    private boolean isLegal(
            Board board,
            Move move,
            Color color,
            CastleRights castleRights,
            Position enPassTarget
    ) {
        ValidationContext ctx = new ValidationContext(
                board, move, color, castleRights, enPassTarget
        );

        return moveValidator.validate(ctx).valid();
    }
}