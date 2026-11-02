package chess.feature.matchmaking.controller;

import chess.feature.game.session.GameSession;
import chess.feature.matchmaking.dto.MatchFoundResponse;
import chess.feature.matchmaking.dto.MatchRequest;
import chess.feature.matchmaking.dto.MatchResult;
import chess.feature.matchmaking.enums.MatchStatus;
import chess.feature.matchmaking.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MatchmakingController {

    private static final String MATCH_STATUS_DESTINATION = "/queue/match-status";
    private static final String MATCH_FOUND_DESTINATION = "/queue/match-found";

    private final MatchmakingService matchmakingService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/match/find")
    public void findMatch(
            @Valid @Payload MatchRequest request,
            Principal principal
    ) {
        String userId = principal.getName();

        MatchResult result = matchmakingService.registerPlayer(userId, request);

        // Chưa tìm được đối thủ.
        if (!result.matched()) {
            sendMatchStatus(userId, MatchStatus.SEARCHING);
            return;
        }

        // Đã tìm được đối thủ và tạo game thành công.
        sendMatchFound(result.session());
    }

    @MessageMapping("/match/cancel")
    public void cancelMatch(Principal principal) {
        String userId = principal.getName();

        boolean removed = matchmakingService.removePlayer(userId);

        sendMatchStatus(userId, removed ? MatchStatus.CANCELLED : MatchStatus.NOT_SEARCHING);
    }

    private void sendMatchStatus(String userId, MatchStatus status) {
        messagingTemplate.convertAndSendToUser(
                userId,
                MATCH_STATUS_DESTINATION,
                status
        );
    }

    private void sendMatchFound(GameSession session) {
        String whitePlayerId = session.getWhitePlayerId();
        String blackPlayerId = session.getBlackPlayerId();

        // Response dành riêng cho người chơi quân Trắng.
        MatchFoundResponse whiteResponse = createMatchFoundResponse(
                session,
                whitePlayerId,
                blackPlayerId,
                "WHITE"
        );

        // Response dành riêng cho người chơi quân Đen.
        MatchFoundResponse blackResponse = createMatchFoundResponse(
                session,
                whitePlayerId,
                blackPlayerId,
                "BLACK"
        );

        messagingTemplate.convertAndSendToUser(
                whitePlayerId,
                MATCH_FOUND_DESTINATION,
                whiteResponse
        );

        messagingTemplate.convertAndSendToUser(
                blackPlayerId,
                MATCH_FOUND_DESTINATION,
                blackResponse
        );
    }

    private MatchFoundResponse createMatchFoundResponse(
            GameSession session,
            String whitePlayerId,
            String blackPlayerId,
            String assignedColor
    ) {
        return new MatchFoundResponse(
                session.getGameId(),

                whitePlayerId,
                null,
                null,

                blackPlayerId,
                null,
                null,

                assignedColor
        );
    }
}