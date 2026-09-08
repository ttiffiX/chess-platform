package chess.chess_engine.model.piece;

import chess.chess_engine.model.Position;

public class Queen extends Piece {
    public Queen(Color color) {
        super(color);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        int fileDiff = Math.abs(from.file() - to.file());
        int rankDiff = Math.abs(from.rank() - to.rank());

        return (fileDiff == 0 || rankDiff == 0 || fileDiff == rankDiff) && !(fileDiff == 0 && rankDiff == 0);
    }
}
