package chess.feature.game.model;

import chess.engine.model.Move;
import chess.engine.model.piece.Piece;

public record Record(
        Move move,
        String fen,
        Piece capturedPiece,
        boolean inCheck,
        Integer moverClockAfterMs
) {
}
