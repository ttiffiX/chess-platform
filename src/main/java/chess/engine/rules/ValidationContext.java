package chess.engine.rules;

import chess.engine.model.Board;
import chess.engine.model.game.CastleRights;
import chess.engine.model.Move;
import chess.engine.model.piece.Color;
import chess.engine.model.Position;

import java.util.Objects;

public record ValidationContext(
        Board board,
        Move move,
        Color turn,
        CastleRights castleRights,
        Position enPassTarget
) {
    public ValidationContext {
        Objects.requireNonNull(board, "Board must not be null.");
        Objects.requireNonNull(move, "Move must not be null.");
        Objects.requireNonNull(turn, "Turn must not be null.");
    }
}