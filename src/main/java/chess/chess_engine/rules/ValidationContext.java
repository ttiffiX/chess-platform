package chess.chess_engine.rules;

import chess.chess_engine.model.Board;
import chess.chess_engine.model.game.CastleRights;
import chess.chess_engine.model.Move;
import chess.chess_engine.model.piece.Color;
import chess.chess_engine.model.Position;

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