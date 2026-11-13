
package chess.game.controller;

import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.controller.GameWebSocketController;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameEventResponse;
import chess.feature.game.dto.response.GameUpdateResponse;
import chess.feature.game.model.Record;
import chess.feature.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameWebSocketController gameWebSocketController;

    private static final String GAME_ID = "room-test-123";
    private static final String WHITE_ID = "user-white";
    private static final String BLACK_ID = "user-black";
    private static final String INTRUDER_ID = "user-intruder";
    private static final String GAME_TOPIC = "/topic/game/" + GAME_ID;

    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final Principal principalWhite = () -> WHITE_ID;
    private final Principal principalBlack = () -> BLACK_ID;
    private final Principal principalIntruder = () -> INTRUDER_ID;

    private GameUpdateResponse createUpdateResponse() {
        return new GameUpdateResponse(
                GAME_ID,
                GameResult.inProgress(),
                false,
                INITIAL_FEN,
                new Record(
                        "e2e4",
                        INITIAL_FEN,
                        null,
                        false,
                        60_000
                ),
                Map.of(),
                60_000L,
                60_000L,
                1_000L,
                2_000L,
                1L
        );
    }

    private GameEventResponse createEventResponse(Color drawOfferedBy) {
        return new GameEventResponse(
                GAME_ID,
                GameResult.inProgress(),
                drawOfferedBy,
                2L
        );
    }

    // ---------------------------------------------------------------------
    // Make move -> GameUpdateResponse
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Make move: broadcast GameUpdateResponse")
    void makeMove_ValidMove_BroadcastsUpdatedState() {
        MoveRequest request = new MoveRequest("e2", "e4", null);
        GameUpdateResponse updatedState = createUpdateResponse();

        when(gameService.makeMove(GAME_ID, request, WHITE_ID))
                .thenReturn(updatedState);

        gameWebSocketController.makeMove(
                GAME_ID,
                request,
                principalWhite
        );

        verify(gameService).makeMove(GAME_ID, request, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, updatedState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Make move: không broadcast khi service ném exception")
    void makeMove_ServiceThrowsException_DoesNotBroadcast() {
        MoveRequest request = new MoveRequest("e2", "e4", null);

        when(gameService.makeMove(GAME_ID, request, INTRUDER_ID))
                .thenThrow(new AppException(
                        ErrorCode.PLAYER_NOT_IN_GAME,
                        "User is not a participant of this game."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.makeMove(
                        GAME_ID,
                        request,
                        principalIntruder
                )
        );

        verify(gameService).makeMove(GAME_ID, request, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }

    // ---------------------------------------------------------------------
    // Draw offer -> GameEventResponse
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Offer draw: broadcast GameEventResponse")
    void offerDraw_ValidRequest_BroadcastsEvent() {
        GameEventResponse event = createEventResponse(Color.WHITE);

        when(gameService.offerDraw(GAME_ID, WHITE_ID))
                .thenReturn(event);

        gameWebSocketController.offerDraw(GAME_ID, principalWhite);

        verify(gameService).offerDraw(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, event);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Offer draw: không broadcast khi service từ chối")
    void offerDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.offerDraw(GAME_ID, WHITE_ID))
                .thenThrow(new AppException(
                        ErrorCode.INVALID_MOVE,
                        "A draw offer is already pending."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.offerDraw(GAME_ID, principalWhite)
        );

        verify(gameService).offerDraw(GAME_ID, WHITE_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Accept draw: broadcast GameEventResponse")
    void acceptDraw_ValidRequest_BroadcastsEvent() {
        GameEventResponse event = createEventResponse(null);

        when(gameService.acceptDraw(GAME_ID, BLACK_ID))
                .thenReturn(event);

        gameWebSocketController.acceptDraw(GAME_ID, principalBlack);

        verify(gameService).acceptDraw(GAME_ID, BLACK_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, event);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Accept draw: không broadcast khi service từ chối")
    void acceptDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.acceptDraw(GAME_ID, BLACK_ID))
                .thenThrow(new AppException(
                        ErrorCode.INVALID_MOVE,
                        "There is no pending draw offer."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.acceptDraw(GAME_ID, principalBlack)
        );

        verify(gameService).acceptDraw(GAME_ID, BLACK_ID);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Decline draw: broadcast GameEventResponse")
    void declineDraw_ValidRequest_BroadcastsEvent() {
        GameEventResponse event = createEventResponse(null);

        when(gameService.declineDraw(GAME_ID, BLACK_ID))
                .thenReturn(event);

        gameWebSocketController.declineDraw(GAME_ID, principalBlack);

        verify(gameService).declineDraw(GAME_ID, BLACK_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, event);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Decline draw: không broadcast khi service từ chối")
    void declineDraw_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.declineDraw(GAME_ID, BLACK_ID))
                .thenThrow(new AppException(
                        ErrorCode.INVALID_MOVE,
                        "There is no pending draw offer."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.declineDraw(GAME_ID, principalBlack)
        );

        verify(gameService).declineDraw(GAME_ID, BLACK_ID);
        verifyNoInteractions(messagingTemplate);
    }

    // ---------------------------------------------------------------------
    // Resign -> GameEventResponse
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Resign: broadcast GameEventResponse")
    void resign_ValidRequest_BroadcastsEvent() {
        GameEventResponse event = createEventResponse(null);

        when(gameService.resign(GAME_ID, WHITE_ID))
                .thenReturn(event);

        gameWebSocketController.resign(GAME_ID, principalWhite);

        verify(gameService).resign(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, event);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Resign: không broadcast khi service từ chối")
    void resign_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.resign(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(
                        ErrorCode.PLAYER_NOT_IN_GAME,
                        "User is not a participant of this game."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.resign(GAME_ID, principalIntruder)
        );

        verify(gameService).resign(GAME_ID, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }

    // ---------------------------------------------------------------------
    // Timeout -> GameEventResponse
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Claim timeout: broadcast GameEventResponse")
    void claimTimeout_ValidRequest_BroadcastsEvent() {
        GameEventResponse event = createEventResponse(null);

        when(gameService.claimTimeout(GAME_ID, WHITE_ID))
                .thenReturn(event);

        gameWebSocketController.claimTimeout(GAME_ID, principalWhite);

        verify(gameService).claimTimeout(GAME_ID, WHITE_ID);
        verify(messagingTemplate).convertAndSend(GAME_TOPIC, event);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Claim timeout: không broadcast khi service từ chối")
    void claimTimeout_ServiceThrowsException_DoesNotBroadcast() {
        when(gameService.claimTimeout(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(
                        ErrorCode.PLAYER_NOT_IN_GAME,
                        "User is not a participant of this game."
                ));

        assertThrows(AppException.class, () ->
                gameWebSocketController.claimTimeout(
                        GAME_ID,
                        principalIntruder
                )
        );

        verify(gameService).claimTimeout(GAME_ID, INTRUDER_ID);
        verifyNoInteractions(messagingTemplate);
    }
}