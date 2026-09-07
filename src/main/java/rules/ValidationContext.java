package rules;

import model.Board;
import model.game.CastleRights;
import model.Move;
import model.piece.Color;
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