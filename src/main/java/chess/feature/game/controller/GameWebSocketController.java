package chess.feature.game.controller;

import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    private String getCurrentUserId(Principal principal) {
        return principal.getName();
    }

    private void broadcastGameState(
            String gameId,
            GameStateResponse state
    ) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId,
                state
        );
    }

    /**
     * 1. Người chơi thực hiện nước đi.
     *
     * Client: /app/game/{gameId}/move
     */
    @MessageMapping("/game/{gameId}/move")
    public void makeMove(
            @DestinationVariable String gameId,
            @Payload MoveRequest request,
            Principal principal
    ) {
        String currentUserId = getCurrentUserId(principal);

        GameStateResponse updatedState = gameService.makeMove(
                gameId,
                request,
                currentUserId
        );

        broadcastGameState(gameId, updatedState);
    }

    /**
     * 2. Người chơi đề nghị hòa.
     *
     * Client: /app/game/{gameId}/draw/offer
     */
    @MessageMapping("/game/{gameId}/draw/offer")
    public void offerDraw(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String senderId = getCurrentUserId(principal);

        // Backend kiểm tra điều kiện và lưu trạng thái đề nghị hòa.
        GameStateResponse updatedState = gameService.offerDraw(
                gameId,
                senderId
        );

        broadcastGameState(gameId, updatedState);
    }

    /**
     * 3. Người chơi chấp nhận đề nghị hòa.
     *
     * Client: /app/game/{gameId}/draw/accept
     */
    @MessageMapping("/game/{gameId}/draw/accept")
    public void acceptDraw(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String currentUserId = getCurrentUserId(principal);

        GameStateResponse updatedState = gameService.acceptDraw(
                gameId,
                currentUserId
        );

        broadcastGameState(gameId, updatedState);
    }

    /**
     * 4. Người chơi từ chối đề nghị hòa.
     *
     * Client: /app/game/{gameId}/draw/decline
     */
    @MessageMapping("/game/{gameId}/draw/decline")
    public void declineDraw(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String senderId = getCurrentUserId(principal);

        // Backend kiểm tra và xóa đề nghị hòa đang chờ.
        GameStateResponse updatedState = gameService.declineDraw(
                gameId,
                senderId
        );

        broadcastGameState(gameId, updatedState);
    }

    /**
     * 5. Người chơi đầu hàng.
     *
     * Client: /app/game/{gameId}/resign
     */
    @MessageMapping("/game/{gameId}/resign")
    public void resign(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String currentUserId = getCurrentUserId(principal);

        GameStateResponse updatedState = gameService.resign(
                gameId,
                currentUserId
        );

        broadcastGameState(gameId, updatedState);
    }

    /**
     * 6. Client yêu cầu backend kiểm tra timeout.
     *
     * Client: /app/game/{gameId}/timeout
     */
    @MessageMapping("/game/{gameId}/timeout")
    public void claimTimeout(
            @DestinationVariable String gameId,
            Principal principal
    ) {
        String currentUserId = getCurrentUserId(principal);

        GameStateResponse updatedState = gameService.claimTimeout(
                gameId,
                currentUserId
        );

        broadcastGameState(gameId, updatedState);
    }
}