package chess.game.controller;

import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.controller.GameWebSocketController;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private static final String GAME_ID = "room-test-123";
    private static final String WHITE_ID = "user-white";
    private static final String BLACK_ID = "user-black";
    private static final String INTRUDER_ID = "user-intruder";
    private static final String GAME_TOPIC = "/topic/game/" + GAME_ID;

    // Principal chỉ cần getName(), không cần mock hay stubbing.
    private final Principal principalWhite = () -> WHITE_ID;
    private final Principal principalBlack = () -> BLACK_ID;
    private final Principal principalIntruder = () -> INTRUDER_ID;

    @InjectMocks
    private GameWebSocketController gameWebSocketController;

    @Test
    @DisplayName("Đi cờ thành công: gọi service và broadcast GameState")
    void makeMove_ValidMove_BroadcastsUpdatedState() {
        MoveRequest request = new MoveRequest("e2", "e4", null);
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.makeMove(GAME_ID, request, WHITE_ID)).thenReturn(updatedState);

        gameWebSocketController.makeMove(GAME_ID, request, principalWhite);

        verify(gameService).makeMove(GAME_ID, request, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Đi cờ bị service từ chối: không broadcast")
    void makeMove_ServiceThrowsException_DoesNotBroadcast() {
        MoveRequest request = new MoveRequest("e2", "e4", null);
        when(gameService.makeMove(GAME_ID, request, INTRUDER_ID))
                .thenThrow(new AppException(
                        ErrorCode.INVALID_MOVE,
                        "User is not a participant of this game."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.makeMove(GAME_ID, request, principalIntruder));

        verify(gameService).makeMove(GAME_ID, request, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Đề nghị hòa: gọi service và broadcast GameState")
    void offerDraw_ValidRequest_BroadcastsUpdatedState() {
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.offerDraw(GAME_ID, WHITE_ID)).thenReturn(updatedState);

        gameWebSocketController.offerDraw(GAME_ID, principalWhite);

        verify(gameService).offerDraw(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Đề nghị hòa bị service từ chối: không broadcast")
    void offerDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.offerDraw(GAME_ID, WHITE_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "A draw offer is already pending."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.offerDraw(GAME_ID, principalWhite));

        verify(gameService).offerDraw(GAME_ID, WHITE_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Chấp nhận hòa: gọi đúng acceptDraw và broadcast GameState")
    void acceptDraw_ValidRequest_BroadcastsUpdatedState() {
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.acceptDraw(GAME_ID, BLACK_ID)).thenReturn(updatedState);

        gameWebSocketController.acceptDraw(GAME_ID, principalBlack);

        verify(gameService).acceptDraw(GAME_ID, BLACK_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Chấp nhận hòa bị service từ chối: không broadcast")
    void acceptDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.acceptDraw(GAME_ID, BLACK_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "There is no pending draw offer."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.acceptDraw(GAME_ID, principalBlack));

        verify(gameService).acceptDraw(GAME_ID, BLACK_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Từ chối hòa: gọi service và broadcast GameState")
    void declineDraw_ValidRequest_BroadcastsUpdatedState() {
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.declineDraw(GAME_ID, BLACK_ID)).thenReturn(updatedState);

        gameWebSocketController.declineDraw(GAME_ID, principalBlack);

        verify(gameService).declineDraw(GAME_ID, BLACK_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Từ chối hòa bị service từ chối: không broadcast")
    void declineDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.declineDraw(GAME_ID, BLACK_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "There is no pending draw offer."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.declineDraw(GAME_ID, principalBlack));

        verify(gameService).declineDraw(GAME_ID, BLACK_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Đầu hàng: gọi service và broadcast trạng thái mới")
    void resign_ValidRequest_BroadcastsUpdatedState() {
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.resign(GAME_ID, WHITE_ID)).thenReturn(updatedState);

        gameWebSocketController.resign(GAME_ID, principalWhite);

        verify(gameService).resign(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Đầu hàng bị service từ chối: không broadcast")
    void resign_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.resign(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "User is not a participant of this game."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.resign(GAME_ID, principalIntruder));

        verify(gameService).resign(GAME_ID, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Yêu cầu kiểm tra timeout: gọi service và broadcast trạng thái")
    void claimTimeout_ValidRequest_BroadcastsCurrentState() {
        GameStateResponse updatedState = mock(GameStateResponse.class);
        when(gameService.claimTimeout(GAME_ID, WHITE_ID)).thenReturn(updatedState);

        gameWebSocketController.claimTimeout(GAME_ID, principalWhite);

        verify(gameService).claimTimeout(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Yêu cầu kiểm tra timeout bị từ chối: không broadcast")
    void claimTimeout_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.claimTimeout(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "User is not a participant of this game."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.claimTimeout(GAME_ID, principalIntruder));

        verify(gameService).claimTimeout(GAME_ID, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }
}
