package chess.feature.game.controller;

import chess.common.dto.ApiResponse;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    // Record phụ dùng để thông báo sự kiện mời/từ chối hòa
    public record DrawEventNotification(String type, String senderId) {
    }

    // =========================================================================
    // 1. CÁC ENDPOINT ĐIỀU KHIỂN CHÍNH QUA WEBSOCKET (@MessageMapping)
    // =========================================================================

    /**
     * 1.1 Thực hiện nước đi từ Client gửi lên: /app/game/{gameId}/move
     */
    @MessageMapping("/game/{gameId}/move")
    public void makeMove(
            @DestinationVariable String gameId,
            @Payload MoveRequest request,
            Principal principal
    ) {
        String currentUserId = principal.getName();

        // Xử lý nước đi và nhận GameStateResponse mới
        GameStateResponse updatedState = gameService.makeMove(gameId, request, currentUserId);

        // Broadcast trạng thái mới nhất cho CẢ HAI người chơi trong phòng
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedState);
    }

    /**
     * 1.2 Người chơi A phát lệnh xin hòa: /app/game/{gameId}/draw/offer
     */
    @MessageMapping("/game/{gameId}/draw/offer")
    public void offerDraw(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String senderId = principal.getName();

        // Bắn thông báo sự kiện "Đề nghị hòa" cho người chơi trong phòng
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/draw-events",
                new DrawEventNotification("DRAW_OFFERED", senderId)
        );
    }

    /**
     * 1.3 Người chơi B chấp nhận hòa: /app/game/{gameId}/draw/accept
     */
    @MessageMapping("/game/{gameId}/draw/accept")
    public void acceptDraw(
            @DestinationVariable String gameId
    ) {
        // Chốt hòa thẳng xuống GameService
        GameStateResponse updatedState = gameService.offerDraw(gameId);

        // Broadcast kết quả ván đấu hòa cho toàn bộ phòng
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedState);
    }

    /**
     * 1.4 Người chơi B từ chối hòa: /app/game/{gameId}/draw/decline
     */
    @MessageMapping("/game/{gameId}/draw/decline")
    public void declineDraw(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String senderId = principal.getName();

        // Bắn sự kiện từ chối hòa để client kia tắt popup chờ
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/draw-events",
                new DrawEventNotification("DRAW_DECLINED", senderId)
        );
    }

    /**
     * 1.5 Người chơi xin đầu hàng: /app/game/{gameId}/resign
     */
    @MessageMapping("/game/{gameId}/resign")
    public void resign(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String currentUserId = principal.getName();

        GameStateResponse updatedState = gameService.resign(gameId, currentUserId);

        // Broadcast trạng thái ván đấu đã kết thúc do đầu hàng
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedState);
    }

    /**
     * 1.6 Client báo hết giờ: /app/game/{gameId}/timeout
     */
    @MessageMapping("/game/{gameId}/timeout")
    public void claimTimeout(
            @DestinationVariable String gameId
    ) {
        GameStateResponse updatedState = gameService.claimTimeout(gameId);

        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedState);
    }

    // =========================================================================
    // 2. CÁC API REST TRUY TRUYỀN THỐNG BỔ TRỢ (READ-ONLY / INITIAL LOAD)
    // =========================================================================

    /**
     * Lấy thông tin bàn cờ khi user mới F5/vào phòng cờ
     */
    @GetMapping("/api/games/{id}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getGame(@PathVariable String id) {
        GameStateResponse response = gameService.getGame(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Lấy danh sách nước đi hợp lệ khi click chọn 1 quân cờ
     */
    @GetMapping("/api/games/{id}/legal-moves")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getLegalMoves(
            @PathVariable String id
    ) {
        Map<String, List<String>> response = gameService.getLegalMoves(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Xem snapshot lịch sử thế cờ cũ (Read-only)
     */
    @GetMapping("/api/games/{id}/history/{moveIndex}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getSnapshotAt(
            @PathVariable String id,
            @PathVariable int moveIndex
    ) {
        GameStateResponse response = gameService.getGameSnapshotAt(id, moveIndex);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}