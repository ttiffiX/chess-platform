package chess.engine.model.game;

import chess.engine.model.Board;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;

public record FenState(
        Board board,
        Color currentTurn,
        CastleRights castleRights,
        Position enPassTarget,
        int halfMoveClock,
        int fullMoveNumber
) {
}
