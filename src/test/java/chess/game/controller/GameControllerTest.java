package chess.game.controller;

import chess.common.dto.ApiResponse;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.GameResult;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.controller.GameController;
import chess.feature.game.dto.response.GameResponse;
import chess.feature.game.model.Record;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameController gameController;

    private static final String GAME_ID = "room-test-123";
    private static final String WHITE_ID = "user-white";
    private static final String BLACK_ID = "user-black";
    private static final String INTRUDER_ID = "user-intruder";

    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String AFTER_E4_FEN =
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";

    private final Principal principalWhite = () -> WHITE_ID;
    private final Principal principalIntruder = () -> INTRUDER_ID;

    private GameResponse createGameResponse() {
        List<Record> history = List.of(
                new Record(
                        null,
                        INITIAL_FEN,
                        null,
                        false,
                        null
                ),
                new Record(
                        new Move(
                                new Position('e', 2),
                                new Position('e', 4),
                                null
                        ),
                        AFTER_E4_FEN,
                        null,
                        false,
                        60_000
                )
        );

        return new GameResponse(
                GAME_ID,
                GameResult.inProgress(),
                false,
                AFTER_E4_FEN,
                history,
                Map.of("e7", List.of("e5")),
                60_000L,
                60_000L,
                1_000L,
                2_000L,
                WHITE_ID,
                BLACK_ID,
                null,
                2L,
                Instant.parse("2026-11-12T10:00:00Z")
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/games/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET game: trả GameResponse kèm toàn bộ lịch sử cho người chơi hợp lệ")
    void getGame_ValidPlayer_ReturnsGameWithHistory() {
        GameResponse expectedResponse = createGameResponse();

        when(gameService.getGame(GAME_ID, WHITE_ID))
                .thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<GameResponse>> response =
                gameController.getGame(GAME_ID, principalWhite);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        GameResponse actualResponse = response.getBody().data();

        assertSame(expectedResponse, actualResponse);
        assertEquals(GAME_ID, actualResponse.gameId());
        assertEquals(AFTER_E4_FEN, actualResponse.fen());

        // Controller trả nguyên GameResponse, bao gồm cả history.
        assertNotNull(actualResponse.historyItems());
        assertEquals(2, actualResponse.historyItems().size());

        assertNull(actualResponse.historyItems().get(0).move());
        assertEquals(
                "e4",
                actualResponse.historyItems().get(1).move().to().toString()
        );

        verify(gameService).getGame(GAME_ID, WHITE_ID);
        verifyNoMoreInteractions(gameService);
    }

    @Test
    @DisplayName("GET game: service từ chối người không thuộc ván cờ")
    void getGame_UnauthorizedPlayer_PropagatesException() {
        when(gameService.getGame(GAME_ID, INTRUDER_ID))
                .thenThrow(new AppException(
                        ErrorCode.PLAYER_NOT_IN_GAME,
                        "User is not a participant of this game."
                ));

        assertThrows(AppException.class, () ->
                gameController.getGame(GAME_ID, principalIntruder));

        verify(gameService).getGame(GAME_ID, INTRUDER_ID);
        verifyNoMoreInteractions(gameService);
    }

    @Test
    @DisplayName("GET game: gameId không tồn tại thì chuyển tiếp lỗi từ service")
    void getGame_GameNotFound_PropagatesException() {
        when(gameService.getGame(GAME_ID, WHITE_ID))
                .thenThrow(new AppException(
                        ErrorCode.ROOM_NOT_FOUND,
                        "Room not found with ID: " + GAME_ID
                ));

        assertThrows(AppException.class, () ->
                gameController.getGame(GAME_ID, principalWhite));

        verify(gameService).getGame(GAME_ID, WHITE_ID);
        verifyNoMoreInteractions(gameService);
    }

    // -------------------------------------------------------------------------
    // GET /api/games/active
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET active game: trả về gameId khi người chơi đang trong ván")
    void getActiveGame_PlayerHasActiveGame_ReturnsGameId() {
        when(gameService.getActiveGameId(WHITE_ID))
                .thenReturn(GAME_ID);

        ResponseEntity<ApiResponse<String>> response =
                gameController.getActiveGame(principalWhite);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(GAME_ID, response.getBody().data());

        verify(gameService).getActiveGameId(WHITE_ID);
        verifyNoMoreInteractions(gameService);
    }

    @Test
    @DisplayName("GET active game: trả về null khi người chơi không có ván đang hoạt động")
    void getActiveGame_PlayerHasNoActiveGame_ReturnsNull() {
        when(gameService.getActiveGameId(WHITE_ID))
                .thenReturn(null);

        ResponseEntity<ApiResponse<String>> response =
                gameController.getActiveGame(principalWhite);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().data());

        verify(gameService).getActiveGameId(WHITE_ID);
        verifyNoMoreInteractions(gameService);
    }
}