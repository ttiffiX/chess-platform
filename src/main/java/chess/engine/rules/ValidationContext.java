package chess.engine.rules;

import chess.engine.model.Board;
import chess.engine.model.game.CastleRights;
import chess.engine.model.Move;
import chess.engine.model.piece.Color;
import chess.engine.model.Position;

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