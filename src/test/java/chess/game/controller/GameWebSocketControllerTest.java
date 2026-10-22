package chess.game.controller;

import chess.common.dto.ApiResponse;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.controller.GameWebSocketController;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameWebSocketControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principalWhite;

    @Mock
    private Principal principalBlack;

    @Mock
    private Principal principalIntruder;

    @InjectMocks
    private GameWebSocketController gameWebSocketController;

    private final String GAME_ID = "room-test-123";
    private final String WHITE_ID = "user-white";
    private final String BLACK_ID = "user-black";
    private final String INTRUDER_ID = "user-intruder";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(principalWhite.getName()).thenReturn(WHITE_ID);
        when(principalBlack.getName()).thenReturn(BLACK_ID);
        when(principalIntruder.getName()).thenReturn(INTRUDER_ID);
    }

    // =========================================================================
    // 1. TEST KHÔI PHỤC VÁN ĐẤU KHI F5 VÀ XEM TRẠNG THÁI (REST APIs)
    // =========================================================================

    @Test
    @DisplayName("F5 Trang web: Lấy lại Game State ổn định qua API GET /api/games/{id}")
    void testGetGame_OnPageRefresh_ReturnsStateSuccessfully() {
        GameStateResponse mockState = mock(GameStateResponse.class);
        when(gameService.getGame(GAME_ID)).thenReturn(mockState);

        ResponseEntity<ApiResponse<GameStateResponse>> response = gameWebSocketController.getGame(GAME_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockState, response.getBody().data());
        verify(gameService, times(1)).getGame(GAME_ID);
    }

    // =========================================================================
    // 2. TEST NƯỚC ĐI & BẢO MẬT USER ID
    // =========================================================================

    @Test
    @DisplayName("Nước đi hợp lệ: Broadcast GameState mới tới /topic/game/{gameId}")
    void testMakeMove_ValidMove_BroadcastsUpdatedState() {
        MoveRequest moveRequest = new MoveRequest("e2", "e4", null);
        GameStateResponse updatedState = mock(GameStateResponse.class);

        when(gameService.makeMove(GAME_ID, moveRequest, WHITE_ID)).thenReturn(updatedState);

        gameWebSocketController.makeMove(GAME_ID, moveRequest, principalWhite);

        verify(gameService, times(1)).makeMove(GAME_ID, moveRequest, WHITE_ID);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/game/" + GAME_ID, updatedState);
    }

    @Test
    @DisplayName("Kẻ lạ (Intruder) hoặc sai lượt cố tình đi cờ: Bị ném ngoại lệ từ Service, không Broadcast")
    void testMakeMove_UnauthorizedUser_ThrowsExceptionAndNoBroadcast() {
        MoveRequest moveRequest = new MoveRequest("e2", "e4", null);

        when(gameService.makeMove(GAME_ID, moveRequest, INTRUDER_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "Not your turn or you are not a player in this room."));

        assertThrows(AppException.class, () ->
                gameWebSocketController.makeMove(GAME_ID, moveRequest, principalIntruder)
        );

        verify(messagingTemplate, never()).convertAndSend(eq("/topic/game/" + GAME_ID), any(Object.class));
    }

    // =========================================================================
    // 3. TEST LUỒNG ĐỀ NGHỊ HÒA (OFFER / ACCEPT / DECLINE / BƠ)
    // =========================================================================

    @Test
    @DisplayName("Gửi đề nghị hòa: Bắn thông báo DRAW_OFFERED tới channel draw-events")
    void testOfferDraw_BroadcastsDrawOfferedEvent() {
        gameWebSocketController.offerDraw(GAME_ID, principalWhite);

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + GAME_ID + "/draw-events"),
                argThat((GameWebSocketController.DrawEventNotification note) ->
                        "DRAW_OFFERED".equals(note.type()) && WHITE_ID.equals(note.senderId())
                )
        );
    }

    @Test
    @DisplayName("Đồng ý hòa (Accept Draw): Gọi Service đồng ý hòa và Broadcast ván đấu kết thúc")
    void testAcceptDraw_AgreesDrawAndBroadcastsFinalState() {
        GameStateResponse drawnState = mock(GameStateResponse.class);
        when(gameService.offerDraw(GAME_ID)).thenReturn(drawnState);

        gameWebSocketController.acceptDraw(GAME_ID);

        verify(gameService, times(1)).offerDraw(GAME_ID);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/game/" + GAME_ID, drawnState);
    }

    @Test
    @DisplayName("Từ chối hòa (Decline Draw): Bắn sự kiện DRAW_DECLINED để ẩn popup bên phía người mời")
    void testDeclineDraw_BroadcastsDrawDeclinedEvent() {
        gameWebSocketController.declineDraw(GAME_ID, principalBlack);

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + GAME_ID + "/draw-events"),
                argThat((GameWebSocketController.DrawEventNotification note) ->
                        "DRAW_DECLINED".equals(note.type()) && BLACK_ID.equals(note.senderId())
                )
        );
        // Không có thay đổi gì ở GameService
        verifyNoInteractions(gameService);
    }

    // =========================================================================
    // 4. TEST ĐẦU HÀNG & HẾT GIỜ (RESIGN / TIMEOUT)
    // =========================================================================

    @Test
    @DisplayName("Người chơi xin đầu hàng: Kết thúc ván đấu và Broadcast trạng thái")
    void testResign_ValidPlayer_BroadcastsResignState() {
        GameStateResponse resignedState = mock(GameStateResponse.class);
        when(gameService.resign(GAME_ID, WHITE_ID)).thenReturn(resignedState);

        gameWebSocketController.resign(GAME_ID, principalWhite);

        verify(gameService, times(1)).resign(GAME_ID, WHITE_ID);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/game/" + GAME_ID, resignedState);
    }

    @Test
    @DisplayName("Client gửi tín hiệu Timeout: Server kiểm tra giờ chính xác và Broadcast kết quả")
    void testClaimTimeout_BroadcastsTimeoutResult() {
        GameStateResponse timeoutState = mock(GameStateResponse.class);
        when(gameService.claimTimeout(GAME_ID)).thenReturn(timeoutState);

        gameWebSocketController.claimTimeout(GAME_ID);

        verify(gameService, times(1)).claimTimeout(GAME_ID);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/game/" + GAME_ID, timeoutState);
    }}