import piece.Color;
import piece.Piece;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Board board;
    private Color currentTurn;
    private final List<MoveRecord> moveHistory = new ArrayList<>();

    public Game() {
        this.board = new Board();
        this.currentTurn = Color.WHITE;
    }

    public Board getBoard() {
        return board;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public void nextTurn() {
        currentTurn = currentTurn.opposite();
    }

    public List<MoveRecord> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public void move(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null.");
        }

        ValidationResult validationResult = ChessRules.validateMove(board, move, currentTurn, moveHistory);
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(validationResult.message());
        }

        Piece piece = board.getPiece(move.from());
        Piece capturedPiece = board.getPiece(move.to());
        board.movePiece(move);
        moveHistory.add(new MoveRecord(piece, move, capturedPiece));
        nextTurn();
    }
}
