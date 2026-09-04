//package rules;
//
//import model.Board;
//import model.Move;
//import model.MoveRecord;
//import piece.Color;
//import piece.Piece;
//import model.Position;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class LegalMoveService {
//    private final MoveValidator moveValidator;
//
//    public LegalMoveService(MoveValidator moveValidator) {
//        if (moveValidator == null) {
//            throw new IllegalArgumentException("MoveValidator must not be null.");
//        }
//        this.moveValidator = moveValidator;
//    }
//
//    public List<Move> generateLegalMoves(Board board, Color color, List<MoveRecord> history) {
//        if (board == null || color == null || history == null) {
//            throw new IllegalArgumentException("Board, color and history must not be null.");
//        }
//
//        List<Move> legalMoves = new ArrayList<>();
//        Map<Position, Piece> pieces = board.getPiecesByColor(color);
//
//        for (Position from : pieces.keySet()) {
//            for (char file = 'a'; file <= 'h'; file++) {
//                for (int rank = 1; rank <= 8; rank++) {
//                    Position to = new Position(file, rank);
//                    if (from.equals(to)) {
//                        continue;
//                    }
//
//                    Move move = new Move(from, to);
//                    if (moveValidator.validate(board, move, color, history).valid()) {
//                        legalMoves.add(move);
//                    }
//                }
//            }
//        }
//
//        return legalMoves;
//    }
//
//    public boolean hasAnyLegalMove(Board board, Color color, List<MoveRecord> history) {
//        return !generateLegalMoves(board, color, history).isEmpty();
//    }
//}
