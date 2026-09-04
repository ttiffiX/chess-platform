package rules;

import model.Board;
import model.CastleRights;
import model.Move;
import piece.Color;
import model.Position;

public record ValidationContext(
        Board board,
        Move move,
        Color turn,
        CastleRights castleRights,
        Position enPassTarget
) {
    public ValidationContext {
        if (board == null || move == null || turn == null) {
            throw new IllegalArgumentException("Board, move, and turn must not be null.");
        }
    }
}