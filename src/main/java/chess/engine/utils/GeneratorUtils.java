package chess.engine.utils;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.Piece;

import java.util.List;

import static chess.engine.utils.EnPassantUtils.createEnPassantTargetKey;

public class GeneratorUtils {

    public static String FENGenerate(
            Board board,
            Color currentTurn,
            String castlingRights,
            Position enPassantTarget,
            int halfmoveClock,
            int fullmoveNumber
    ) {
        return generatePiecePlacement(board)
                + " " + (currentTurn == Color.WHITE ? "w" : "b")
                + " " + castlingRights
                + " " + createEnPassantTargetKey(enPassantTarget)
                + " " + halfmoveClock
                + " " + fullmoveNumber;
    }

    public static String generatePositionKey(
            Board board,
            Color currentTurn,
            String castlingRights,
            Position enPassantTarget,
            List<Move> legalMoves
    ) {
        return generatePiecePlacement(board)
                + " " + (currentTurn == Color.WHITE ? "w" : "b")
                + " " + castlingRights
                + " " + createEnPassantTargetKey(board, enPassantTarget, legalMoves);
    }

    private static String generatePiecePlacement(Board board) {
        StringBuilder fen = new StringBuilder();

        for (int rank = 8; rank >= 1; rank--) {
            int emptyCount = 0;

            for (char file = 'a'; file <= 'h'; file++) {
                Position position = new Position(file, rank);
                Piece piece = board.getPiece(position);

                if (piece == null) {
                    emptyCount++;
                    continue;
                }

                if (emptyCount > 0) {
                    fen.append(emptyCount);
                    emptyCount = 0;
                }

                char symbol = piece.getType().getUciChar().charAt(0);

                fen.append(piece.getColor() == Color.WHITE
                        ? Character.toUpperCase(symbol)
                        : Character.toLowerCase(symbol));
            }

            if (emptyCount > 0) {
                fen.append(emptyCount);
            }

            if (rank > 1) {
                fen.append('/');
            }
        }

        return fen.toString();
    }
}