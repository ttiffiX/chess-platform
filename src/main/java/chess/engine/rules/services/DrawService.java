package chess.engine.rules.services;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.CastleRights;
import chess.engine.model.piece.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DrawService {
    public boolean isInsufficientMaterial(Board board) {
        Objects.requireNonNull(board, "Board must not be null");

        List<Map.Entry<Position, Piece>> nonKingEntries = board.getPieces().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof King))
                .toList();

        if (nonKingEntries.isEmpty()) {
            return true;
        }

        if (nonKingEntries.size() == 1) {
            Piece onlyPiece = nonKingEntries.getFirst().getValue();
            return onlyPiece instanceof Bishop || onlyPiece instanceof Knight;
        }

        if (nonKingEntries.size() == 2) {
            Map.Entry<Position, Piece> first = nonKingEntries.get(0);
            Map.Entry<Position, Piece> second = nonKingEntries.get(1);

            if (first.getValue() instanceof Bishop && second.getValue() instanceof Bishop) {
                Position firstPosition = first.getKey();
                Position secondPosition = second.getKey();
                return isLightSquare(firstPosition) == isLightSquare(secondPosition);
            }
        }

        return false;
    }

    private boolean isLightSquare(Position pos) {
        return ((pos.file() - 'a') + pos.rank()) % 2 != 0;
    }

    public boolean isFiftyMoveRule(int halfMoveClock) {
        return halfMoveClock >= 100;
    }

    public boolean isThreefoldRepetition(int occurrenceCount) {
        return occurrenceCount >= 3;
    }

    public String createPositionKey(
            Board board,
            Color currentTurn,
            CastleRights castleRights,
            Position enPassTarget,
            List<Move> legalMoves
    ) {
        Objects.requireNonNull(board, "Board must not be null");
        Objects.requireNonNull(currentTurn, "Current turn must not be null");
        Objects.requireNonNull(castleRights, "Castle rights must not be null");
        Objects.requireNonNull(legalMoves, "Legal moves must not be null");

        String piecesKey = board.getPieces().entrySet().stream()
                .sorted(
                        Map.Entry.comparingByKey(
                                Comparator.comparingInt(Position::rank)
                                        .thenComparingInt(Position::file)
                        )
                )
                .map(entry -> {
                    Position position = entry.getKey();
                    Piece piece = entry.getValue();

                    return position + ":"
                            + piece.getColor() + ":"
                            + piece.getType();
                })
                .collect(Collectors.joining(","));

        String castleKey = createCastleRightsKey(castleRights);

        // An en-passant target only changes position identity when
        // a legal en-passant capture is actually available.
        String effectiveEnPassTarget =
                hasLegalEnPassantCapture(board, enPassTarget, legalMoves)
                        ? enPassTarget.toString()
                        : "-";

        return piecesKey
                + "|" + currentTurn
                + "|" + castleKey
                + "|" + effectiveEnPassTarget;
    }

    private boolean hasLegalEnPassantCapture(
            Board board,
            Position enPassTarget,
            List<Move> legalMoves
    ) {
        if (enPassTarget == null || !board.isEmpty(enPassTarget)) {
            return false;
        }

        return legalMoves.stream().anyMatch(move -> {
            if (!move.to().equals(enPassTarget)) {
                return false;
            }

            return board.getPiece(move.from()) instanceof Pawn;
        });
    }

    private String createCastleRightsKey(CastleRights rights) {
        StringBuilder key = new StringBuilder();

        if (rights.whiteKingSide()) key.append('K');
        if (rights.whiteQueenSide()) key.append('Q');
        if (rights.blackKingSide()) key.append('k');
        if (rights.blackQueenSide()) key.append('q');

        return key.isEmpty() ? "-" : key.toString();
    }
}
