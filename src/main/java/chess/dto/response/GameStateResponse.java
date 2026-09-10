package chess.dto.response;

import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GameStateResponse(
        String gameId,
        GameResult gameResult,
        Color currentTurn,
        boolean inCheck,
        int fullMoveNumber,
        Map<String, PieceDto> board,
        List<PieceDto> capturedWhitePieces,
        List<PieceDto> capturedBlackPieces,
        List<MoveHistoryDto> historyItems,
        Map<String, List<String>> legalMoves,
        Instant createdAt
) {}