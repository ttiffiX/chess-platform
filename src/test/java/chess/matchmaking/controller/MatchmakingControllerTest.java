package chess.matchmaking.controller;

import chess.feature.game.session.GameSession;
import chess.feature.matchmaking.controller.MatchmakingController;
import chess.feature.matchmaking.dto.MatchFoundResponse;
import chess.feature.matchmaking.dto.MatchRequest;
import chess.feature.matchmaking.dto.MatchResult;
import chess.feature.matchmaking.enums.MatchStatus;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import chess.feature.matchmaking.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingControllerTest {

    private static final String MATCH_STATUS_DESTINATION =
            "/queue/match-status";

    private static final String MATCH_FOUND_DESTINATION =
            "/queue/match-found";

    @Mock
    private MatchmakingService matchmakingService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameSession gameSession;

    @InjectMocks
    private MatchmakingController matchmakingController;

    @Test
    void findMatch_shouldSendSearchingStatus_whenNoOpponentFound() {
        // Arrange
        String userId = "player-1";

        MatchRequest request =
                new MatchRequest(MatchmakingTimeControlPreset.BLITZ_3_0);

        Principal principal = principal(userId);

        when(matchmakingService.registerPlayer(userId, request))
                .thenReturn(new MatchResult(false, null, null));

        // Act
        matchmakingController.findMatch(request, principal);

        // Assert
        verify(matchmakingService).registerPlayer(userId, request);

        verify(messagingTemplate).convertAndSendToUser(
                userId,
                MATCH_STATUS_DESTINATION,
                MatchStatus.SEARCHING
        );

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(userId),
                eq(MATCH_FOUND_DESTINATION),
                any()
        );
    }

    @Test
    void findMatch_shouldSendMatchFoundToBothPlayers_whenMatchCreated() {
        // Arrange
        String whitePlayerId = "player-white";
        String blackPlayerId = "player-black";

        MatchRequest request =
                new MatchRequest(MatchmakingTimeControlPreset.BLITZ_3_0);

        when(matchmakingService.registerPlayer("player-2", request))
                .thenReturn(new MatchResult(true, whitePlayerId, gameSession));

        when(gameSession.getGameId()).thenReturn("game-123");
        when(gameSession.getWhitePlayerId()).thenReturn(whitePlayerId);
        when(gameSession.getBlackPlayerId()).thenReturn(blackPlayerId);

        // Act
        matchmakingController.findMatch(request, principal("player-2"));

        // Assert
        verify(matchmakingService).registerPlayer("player-2", request);

        ArgumentCaptor<String> userCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Object> payloadCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, times(2)).convertAndSendToUser(
                userCaptor.capture(),
                eq(MATCH_FOUND_DESTINATION),
                payloadCaptor.capture()
        );

        List<String> recipients = userCaptor.getAllValues();
        List<Object> payloads = payloadCaptor.getAllValues();

        assertEquals(List.of(whitePlayerId, blackPlayerId), recipients);

        MatchFoundResponse whiteResponse =
                (MatchFoundResponse) payloads.get(0);

        MatchFoundResponse blackResponse =
                (MatchFoundResponse) payloads.get(1);

        // Kiểm tra response của người cầm quân Trắng.
        assertEquals("game-123", whiteResponse.gameId());
        assertEquals(whitePlayerId, whiteResponse.whitePlayerId());
        assertEquals(blackPlayerId, whiteResponse.blackPlayerId());
        assertEquals("WHITE", whiteResponse.assignedColor());

        // Kiểm tra response của người cầm quân Đen.
        assertEquals("game-123", blackResponse.gameId());
        assertEquals(whitePlayerId, blackResponse.whitePlayerId());
        assertEquals(blackPlayerId, blackResponse.blackPlayerId());
        assertEquals("BLACK", blackResponse.assignedColor());

        // Hiện tại chưa tích hợp profile service.
        assertNull(whiteResponse.whitePlayerName());
        assertNull(whiteResponse.whitePlayerAvatar());
        assertNull(whiteResponse.blackPlayerName());
        assertNull(whiteResponse.blackPlayerAvatar());

        assertNull(blackResponse.whitePlayerName());
        assertNull(blackResponse.whitePlayerAvatar());
        assertNull(blackResponse.blackPlayerName());
        assertNull(blackResponse.blackPlayerAvatar());

        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(),
                eq(MATCH_STATUS_DESTINATION),
                any()
        );
    }

    @Test
    void cancelMatch_shouldSendCancelled_whenPlayerWasRemoved() {
        // Arrange
        String userId = "player-1";

        when(matchmakingService.removePlayer(userId))
                .thenReturn(true);

        // Act
        matchmakingController.cancelMatch(principal(userId));

        // Assert
        verify(matchmakingService).removePlayer(userId);

        verify(messagingTemplate).convertAndSendToUser(
                userId,
                MATCH_STATUS_DESTINATION,
                MatchStatus.CANCELLED
        );
    }

    @Test
    void cancelMatch_shouldSendNotSearching_whenPlayerWasNotInQueue() {
        // Arrange
        String userId = "player-1";

        when(matchmakingService.removePlayer(userId))
                .thenReturn(false);

        // Act
        matchmakingController.cancelMatch(principal(userId));

        // Assert
        verify(matchmakingService).removePlayer(userId);

        verify(messagingTemplate).convertAndSendToUser(
                userId,
                MATCH_STATUS_DESTINATION,
                MatchStatus.NOT_SEARCHING
        );
    }

    @Test
    void findMatch_shouldPropagateException_whenServiceFails() {
        // Arrange
        String userId = "player-1";

        MatchRequest request =
                new MatchRequest(MatchmakingTimeControlPreset.BLITZ_3_0);

        when(matchmakingService.registerPlayer(userId, request))
                .thenThrow(new RuntimeException("Matchmaking failed"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> matchmakingController.findMatch(
                        request,
                        principal(userId)
                )
        );

        // Không gửi status hoặc match-found khi service thất bại.
        verifyNoInteractions(messagingTemplate);
    }

    private Principal principal(String userId) {
        return () -> userId;
    }
}