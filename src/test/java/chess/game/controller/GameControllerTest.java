package chess.game.controller;

import chess.common.dto.ApiResponse;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.controller.GameController;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    private static final String GAME_ID = "room-test-123";
    private static final String WHITE_ID = "user-white";
    private static final String INTRUDER_ID = "user-intruder";

    private final Principal principalWhite = () -> WHITE_ID;
    private final Principal principalIntruder = () -> INTRUDER_ID;

    @InjectMocks
    private GameController gameController;

    @Test
    @DisplayName("GET game: trả GameState cho người chơi hợp lệ")
    void getGame_ValidPlayer_ReturnsGameState() {
        GameStateResponse expectedState = mock(GameStateResponse.class);
        when(gameService.getGame(GAME_ID, WHITE_ID)).thenReturn(expectedState);

        ResponseEntity<ApiResponse<GameStateResponse>> response =
                gameController.getGame(GAME_ID, principalWhite);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(expectedState, response.getBody().data());
        verify(gameService).getGame(GAME_ID, WHITE_ID);
        verifyNoMoreInteractions(gameService);
    }

    @Test
    @DisplayName("GET game: service từ chối người không thuộc ván cờ")
    void getGame_UnauthorizedPlayer_PropagatesException() {
        when(gameService.getGame(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "User is not a participant of this game."));

        assertThrows(AppException.class, () ->
                gameController.getGame(GAME_ID, principalIntruder));

        verify(gameService).getGame(GAME_ID, INTRUDER_ID);
    }

    @Test
    @DisplayName("GET game: gameId không tồn tại thì chuyển tiếp lỗi từ service")
    void getGame_GameNotFound_PropagatesException() {
        when(gameService.getGame(GAME_ID, WHITE_ID))
                .thenThrow(new AppException(ErrorCode.ROOM_NOT_FOUND, "Room not found with ID: " + GAME_ID));

        assertThrows(AppException.class, () ->
                gameController.getGame(GAME_ID, principalWhite));

        verify(gameService).getGame(GAME_ID, WHITE_ID);
    }

    @Test
    @DisplayName("GET snapshot: gọi service theo thứ tự gameId, playerId, moveIndex")
    void getSnapshotAt_ValidPlayer_ReturnsSnapshot() {
        int moveIndex = 3;
        GameStateResponse expectedSnapshot = mock(GameStateResponse.class);
        when(gameService.getGameSnapshotAt(GAME_ID, WHITE_ID, moveIndex))
                .thenReturn(expectedSnapshot);

        ResponseEntity<ApiResponse<GameStateResponse>> response =
                gameController.getSnapshotAt(GAME_ID, moveIndex, principalWhite);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertSame(expectedSnapshot, response.getBody().data());
        verify(gameService).getGameSnapshotAt(GAME_ID, WHITE_ID, moveIndex);
        verifyNoMoreInteractions(gameService);
    }

    @Test
    @DisplayName("GET snapshot: service từ chối user không thuộc ván cờ")
    void getSnapshotAt_UnauthorizedPlayer_PropagatesException() {
        int moveIndex = 0;
        when(gameService.getGameSnapshotAt(GAME_ID, INTRUDER_ID, moveIndex))
                .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "User is not a participant of this game."));

        assertThrows(AppException.class, () ->
                gameController.getSnapshotAt(GAME_ID, moveIndex, principalIntruder));

        verify(gameService).getGameSnapshotAt(GAME_ID, INTRUDER_ID, moveIndex);
    }

    @Test
    @DisplayName("GET snapshot: moveIndex không hợp lệ thì chuyển tiếp lỗi từ service")
    void getSnapshotAt_InvalidMoveIndex_PropagatesException() {
        int invalidMoveIndex = -1;
        when(gameService.getGameSnapshotAt(GAME_ID, WHITE_ID, invalidMoveIndex))
                .thenThrow(new AppException(ErrorCode.INVALID_INPUT, "Invalid move index: -1"));

        assertThrows(AppException.class, () ->
                gameController.getSnapshotAt(GAME_ID, invalidMoveIndex, principalWhite));

        verify(gameService).getGameSnapshotAt(GAME_ID, WHITE_ID, invalidMoveIndex);
    }
}
