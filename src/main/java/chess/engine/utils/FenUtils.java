package chess.engine.utils;

import chess.engine.model.Board;
import chess.engine.model.Position;
import chess.engine.model.game.CastleRights;
import chess.engine.model.game.FenState;
import chess.engine.model.piece.*;

import java.util.HashMap;
import java.util.Map;

public class FenUtils {
    public static FenState parseFen(String fen) {
        if (fen == null || fen.isBlank()) {
            throw invalidFen("FEN must not be null or blank.");
        }

        String[] fields = fen.trim().split("\\s+");

        if (fields.length != 6) {
            throw invalidFen("FEN must contain exactly 6 fields.");
        }

        Board board = parseBoard(fields[0]);

        Color currentTurn = switch (fields[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw invalidFen("Invalid active color.");
        };

        CastleRights castleRights = parseCastleRights(fields[2], board);

        Position enPassTarget = parseEnPassTarget(
                fields[3],
                currentTurn,
                board
        );

        int halfMoveClock = parseCounter(fields[4], "halfmove clock", 0);
        int fullMoveNumber = parseCounter(fields[5], "fullmove number", 1);

        return new FenState(
                board,
                currentTurn,
                castleRights,
                enPassTarget,
                halfMoveClock,
                fullMoveNumber
        );
    }

    private static Board parseBoard(String placement) {
        String[] ranks = placement.split("/", -1);

        if (ranks.length != 8) {
            throw invalidFen("Board must contain exactly 8 ranks.");
        }

        Map<Position, Piece> pieces = new HashMap<>();

        for (int row = 0; row < 8; row++) {
            String rank = ranks[row];
            int file = 0;

            for (char symbol : rank.toCharArray()) {
                if (symbol >= '1' && symbol <= '8') {
                    file += symbol - '0';
                    continue;
                }

                if (file >= 8) {
                    throw invalidFen("Too many squares in rank.");
                }

                Piece piece = parsePiece(symbol);

                Position position = new Position(
                        (char) ('a' + file),
                        8 - row
                );

                pieces.put(position, piece);
                file++;
            }

            if (file != 8) {
                throw invalidFen("Each rank must contain exactly 8 squares.");
            }
        }

        Board board = new Board(pieces);

        long whiteKings = pieces.values().stream()
                .filter(piece -> piece instanceof King)
                .filter(piece -> piece.getColor() == Color.WHITE)
                .count();

        long blackKings = pieces.values().stream()
                .filter(piece -> piece instanceof King)
                .filter(piece -> piece.getColor() == Color.BLACK)
                .count();

        if (whiteKings != 1 || blackKings != 1) {
            throw invalidFen("Board must contain exactly one king of each color.");
        }

        // Pawns cannot remain on the first or eighth rank.
        for (Map.Entry<Position, Piece> entry : pieces.entrySet()) {
            Piece piece = entry.getValue();
            int rank = entry.getKey().rank();

            if (piece instanceof Pawn && (rank == 1 || rank == 8)) {
                throw invalidFen("Pawn cannot be placed on rank 1 or 8.");
            }
        }

        return board;
    }

    private static Piece parsePiece(char symbol) {
        Color color = Character.isUpperCase(symbol)
                ? Color.WHITE
                : Color.BLACK;

        PieceType type = PieceType.fromUciChar(String.valueOf(symbol));

        return type.getPiece(color);
    }

    private static CastleRights parseCastleRights(String field, Board board) {
        CastleRights rights = CastleRights.createCastleRightsFromKey(field);

        boolean whiteKingSide = rights.whiteKingSide();
        boolean whiteQueenSide = rights.whiteQueenSide();
        boolean blackKingSide = rights.blackKingSide();
        boolean blackQueenSide = rights.blackQueenSide();

        // Castling rights must agree with the actual king and rook positions.
        if (whiteKingSide || whiteQueenSide) {
            requirePiece(board, "e1", PieceType.KING, Color.WHITE);
        }

        if (whiteKingSide) {
            requirePiece(board, "h1", PieceType.ROOK, Color.WHITE);
        }

        if (whiteQueenSide) {
            requirePiece(board, "a1", PieceType.ROOK, Color.WHITE);
        }

        if (blackKingSide || blackQueenSide) {
            requirePiece(board, "e8", PieceType.KING, Color.BLACK);
        }

        if (blackKingSide) {
            requirePiece(board, "h8", PieceType.ROOK, Color.BLACK);
        }

        if (blackQueenSide) {
            requirePiece(board, "a8", PieceType.ROOK, Color.BLACK);
        }

        return rights;
    }

    private static void requirePiece(
            Board board,
            String square,
            PieceType expectedType,
            Color expectedColor
    ) {
        Position position = new Position(square.charAt(0), square.charAt(1) - '0');
        Piece piece = board.getPiece(position);

        if (piece == null
                || piece.getType() != expectedType
                || piece.getColor() != expectedColor) {
            throw invalidFen("Invalid piece for castling right at " + square);
        }
    }

    private static Position parseEnPassTarget(
            String field,
            Color currentTurn,
            Board board
    ) {
        if (field.equals("-")) {
            return null;
        }

        if (!field.matches("[a-h][36]")) {
            throw invalidFen("Invalid en passant target.");
        }

        Position target = new Position(
                field.charAt(0),
                field.charAt(1) - '0'
        );

        int expectedRank = currentTurn == Color.WHITE ? 6 : 3;

        if (target.rank() != expectedRank) {
            throw invalidFen("Invalid en passant target rank.");
        }

        if (!board.isEmpty(target)) {
            throw invalidFen("En passant target square must be empty.");
        }

        // Verify that the opponent's pawn has just moved two squares.
        int pawnRank = currentTurn == Color.WHITE
                ? target.rank() - 1
                : target.rank() + 1;

        int originRank = currentTurn == Color.WHITE
                ? target.rank() + 1
                : target.rank() - 1;

        Piece movedPawn = board.getPiece(
                new Position(target.file(), pawnRank)
        );

        if (!(movedPawn instanceof Pawn)
                || movedPawn.getColor() != currentTurn.opposite()) {
            throw invalidFen("En passant target has no corresponding pawn.");
        }

        if (!board.isEmpty(new Position(target.file(), originRank))) {
            throw invalidFen("En passant pawn origin square must be empty.");
        }

        return target;
    }

    private static int parseCounter(String value, String name, int minValue) {
        if (!value.matches("\\d+")) {
            throw invalidFen("Invalid " + name + ".");
        }

        try {
            int result = Integer.parseInt(value);

            if (result < minValue) {
                throw invalidFen("Invalid " + name + ".");
            }

            return result;
        } catch (NumberFormatException exception) {
            throw invalidFen(name + " is out of range.");
        }
    }

    private static IllegalArgumentException invalidFen(String message) {
        return new IllegalArgumentException("Invalid FEN: " + message);
    }
}
