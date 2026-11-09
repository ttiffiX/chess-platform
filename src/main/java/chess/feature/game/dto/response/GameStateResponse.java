package chess.feature.game.dto.response;

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

        Long whiteRemainingMs,
        Long blackRemainingMs,
        Long turnStartTimeEpochMs,
        Long serverTimeEpochMs,

        // New fields for player IDs
        String whitePlayerId,
        String blackPlayerId,
        Color drawOfferedBy,

        long stateVersion, // Phiên bản trạng thái của game session, dùng để đồng bộ trạng thái giữa server và client.
        Instant createdAt
) {}