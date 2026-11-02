package chess.feature.matchmaking.dto;

public record MatchFoundResponse(
        String gameId,          // Mã phòng chơi để redirect

        // Thông tin quân Trắng (WHITE)
        String whitePlayerId,
        String whitePlayerName,
        String whitePlayerAvatar,

        // Thông tin quân Đen (BLACK)
        String blackPlayerId,
        String blackPlayerName,
        String blackPlayerAvatar,

        // Màu quân mà người nhận được gói tin này sẽ cầm ("WHITE" hoặc "BLACK")
        String assignedColor
) {}