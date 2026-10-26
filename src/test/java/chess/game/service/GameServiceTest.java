package chess.game.service;

import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import chess.feature.game.session.ChessClock;
import chess.feature.game.session.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private static final String PLAYER_1_ID = "user-1";
    private static final String PLAYER_2_ID = "user-2";
    private static final String INTRUDER_ID = "user-intruder";
    private static final long BASE_TIME_SECONDS = 60L;
    private static final long INCREMENT_SECONDS = 1L;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    private GameSession createTimedGame() {
        return gameService.createGame(new CreateGameRequest(
                BASE_TIME_SECONDS,
                INCREMENT_SECONDS,
                PLAYER_1_ID,
                PLAYER_2_ID
        ));
    }

    /**
     * Access the session's private clock only for tests that need to control
     * elapsed time. Normal service behavior is tested through public methods.
     */
    private ChessClock clockOf(GameSession session) {
        Object clock = ReflectionTestUtils.getField(session, "clock");
        assertNotNull(clock, "GameSession should contain a ChessClock field named 'clock'.");
        return (ChessClock) clock;
    }

    private void forceClockExpired(GameSession session) {
        ChessClock clock = clockOf(session);
        ReflectionTestUtils.setField(
                clock,
                "lastTimestamp",
                Instant.now().minusSeconds(BASE_TIME_SECONDS + 60)
        );
    }

    @Test
    @DisplayName("Create game: gán hai màu khác nhau và khởi động clock")
    void createGame_AssignsPlayersAndStartsClock() {
        GameSession session = createTimedGame();

        assertNotNull(session.getGameId());
        assertNotEquals(session.getWhitePlayerId(), session.getBlackPlayerId());
        assertTrue(
                session.getWhitePlayerId().equals(PLAYER_1_ID)
                        || session.getWhitePlayerId().equals(PLAYER_2_ID)
        );
        assertTrue(clockOf(session).isRunning());
        assertEquals(session.getGame().getCurrentTurn(), clockOf(session).getActiveColor());
        assertNotNull(session.getTurnStartedAt());
    }

    @Test
    @DisplayName("Create game: không cho increment khác 0 khi base time bằng 0")
    void createGame_ZeroBaseWithIncrement_ThrowsException() {
        CreateGameRequest request = new CreateGameRequest(
                0L,
                1L,
                PLAYER_1_ID,
                PLAYER_2_ID
        );

        assertThrows(AppException.class, () -> gameService.createGame(request));
    }

    @Test
    @DisplayName("Get game: người chơi trong ván được lấy trạng thái")
    void getGame_PlayerInGame_ReturnsState() {
        GameSession session = createTimedGame();

        GameStateResponse response = gameService.getGame(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertEquals(session.getGameId(), response.gameId());
        assertEquals(Color.WHITE, response.currentTurn());
    }

    @Test
    @DisplayName("Get game: người ngoài ván bị từ chối")
    void getGame_UnknownPlayer_ThrowsException() {
        GameSession session = createTimedGame();

        assertThrows(AppException.class, () ->
                gameService.getGame(session.getGameId(), INTRUDER_ID));
    }

    @Test
    @DisplayName("Make move: nước đi hợp lệ chuyển lượt và đổi màu clock")
    void makeMove_ValidMove_UpdatesGameAndClock() {
        GameSession session = createTimedGame();
        String whiteId = session.getWhitePlayerId();

        GameStateResponse response = gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e2", "e4", null),
                whiteId
        );

        assertEquals(Color.BLACK, response.currentTurn());
        assertTrue(response.board().containsKey("e4"));
        assertFalse(response.board().containsKey("e2"));
        assertEquals(Color.BLACK, clockOf(session).getActiveColor());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Make move: nước đi không hợp lệ không được áp dụng")
    void makeMove_InvalidMove_LeavesTurnUnchanged() {
        GameSession session = createTimedGame();
        String whiteId = session.getWhitePlayerId();

        assertThrows(AppException.class, () -> gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e2", "e5", null),
                whiteId
        ));

        assertEquals(Color.WHITE, session.getGame().getCurrentTurn());
        assertEquals(Color.WHITE, clockOf(session).getActiveColor());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Make move: người chơi không đúng lượt bị từ chối")
    void makeMove_WrongTurn_ThrowsException() {
        GameSession session = createTimedGame();

        assertThrows(AppException.class, () -> gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e7", "e5", null),
                session.getBlackPlayerId()
        ));

        assertEquals(Color.WHITE, session.getGame().getCurrentTurn());
    }

    @Test
    @DisplayName("Make move: timeout được xử lý trước nước đi")
    void makeMove_ClockExpired_EndsGameWithoutApplyingMove() {
        GameSession session = createTimedGame();
        forceClockExpired(session);

        GameStateResponse response = gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e2", "e4", null),
                session.getWhitePlayerId()
        );

        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertEquals(Color.WHITE, session.getGame().getCurrentTurn());
        assertTrue(response.gameResult().status() != GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Resign: kết thúc ván, dừng clock và xóa draw offer")
    void resign_ValidPlayer_EndsGameAndStopsClock() {
        GameSession session = createTimedGame();
        session.offerDraw(Color.WHITE);

        GameStateResponse response = gameService.resign(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertNull(session.getDrawOfferedBy());
        assertNotNull(response);
    }

    @Test
    @DisplayName("Offer draw: lưu người đề nghị nhưng ván vẫn tiếp tục và clock vẫn chạy")
    void offerDraw_ValidPlayer_StoresOfferWithoutEndingGame() {
        GameSession session = createTimedGame();

        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertEquals(Color.WHITE, session.getDrawOfferedBy());
        assertFalse(session.getGame().isGameOver());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Không cho phép có hai đề nghị hòa đang chờ cùng lúc")
    void offerDraw_OfferAlreadyPending_ThrowsException() {
        GameSession session = createTimedGame();
        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertThrows(AppException.class, () ->
                gameService.offerDraw(session.getGameId(), session.getBlackPlayerId()));

        assertEquals(Color.WHITE, session.getDrawOfferedBy());
    }

    @Test
    @DisplayName("Accept draw: kết thúc ván, dừng clock và xóa đề nghị")
    void acceptDraw_OfferedByOpponent_EndsGame() {
        GameSession session = createTimedGame();
        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        GameStateResponse response = gameService.acceptDraw(
                session.getGameId(),
                session.getBlackPlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertNull(session.getDrawOfferedBy());
    }

    @Test
    @DisplayName("Người đề nghị không thể tự chấp nhận hòa")
    void acceptDraw_OwnOffer_ThrowsException() {
        GameSession session = createTimedGame();
        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertThrows(AppException.class, () ->
                gameService.acceptDraw(session.getGameId(), session.getWhitePlayerId()));

        assertFalse(session.getGame().isGameOver());
        assertEquals(Color.WHITE, session.getDrawOfferedBy());
    }

    @Test
    @DisplayName("Decline draw: xóa đề nghị nhưng không kết thúc ván hoặc dừng clock")
    void declineDraw_OfferedByOpponent_ClearsOfferOnly() {
        GameSession session = createTimedGame();
        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        gameService.declineDraw(session.getGameId(), session.getBlackPlayerId());

        assertNull(session.getDrawOfferedBy());
        assertFalse(session.getGame().isGameOver());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Người đề nghị không thể tự từ chối đề nghị hòa của mình")
    void declineDraw_OwnOffer_ThrowsException() {
        GameSession session = createTimedGame();
        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertThrows(AppException.class, () ->
                gameService.declineDraw(session.getGameId(), session.getWhitePlayerId()));

        assertEquals(Color.WHITE, session.getDrawOfferedBy());
    }

    @Test
    @DisplayName("Claim timeout trước khi hết giờ không kết thúc ván")
    void claimTimeout_NotExpired_KeepsGameInProgress() {
        GameSession session = createTimedGame();

        GameStateResponse response = gameService.claimTimeout(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertFalse(session.getGame().isGameOver());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Claim timeout khi hết giờ kết thúc ván và dừng clock")
    void claimTimeout_Expired_EndsGameAndStopsClock() {
        GameSession session = createTimedGame();
        forceClockExpired(session);

        GameStateResponse response = gameService.claimTimeout(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertTrue(response.gameResult().status() != GameStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Snapshot lịch sử: thành viên hợp lệ xem được snapshot đầu tiên")
    void getGameSnapshotAt_ValidPlayer_ReturnsSnapshot() {
        GameSession session = createTimedGame();

        GameStateResponse snapshot = gameService.getGameSnapshotAt(
                session.getGameId(),
                session.getWhitePlayerId(),
                0
        );

        assertNotNull(snapshot);
        assertEquals(session.getGameId(), snapshot.gameId());
    }

    @Test
    @DisplayName("Snapshot lịch sử: chỉ số ngoài phạm vi bị từ chối")
    void getGameSnapshotAt_InvalidIndex_ThrowsException() {
        GameSession session = createTimedGame();

        assertThrows(AppException.class, () -> gameService.getGameSnapshotAt(
                session.getGameId(),
                session.getWhitePlayerId(),
                -1
        ));
    }

    @Test
    @DisplayName("Snapshot lịch sử: người ngoài ván bị từ chối")
    void getGameSnapshotAt_UnknownPlayer_ThrowsException() {
        GameSession session = createTimedGame();

        assertThrows(AppException.class, () -> gameService.getGameSnapshotAt(
                session.getGameId(),
                INTRUDER_ID,
                0
        ));
    }

    /**
     * These regression tests intentionally verify that membership is checked
     * before timeout can mutate a game. They expose the current ordering issue
     * if a mutating service method calls checkTimeoutAndUpdate() first.
     */
    @ParameterizedTest(name = "Unknown player cannot trigger timeout through {0}")
    @ValueSource(strings = {"makeMove", "resign", "offerDraw", "acceptDraw", "declineDraw"})
    @DisplayName("Người ngoài không thể kích hoạt timeout qua action thay đổi trạng thái")
    void unknownPlayerCannotTriggerTimeout(String action) {
        GameSession session = createTimedGame();
        forceClockExpired(session);

        assertThrows(AppException.class, () -> {
            switch (action) {
                case "makeMove" -> gameService.makeMove(
                        session.getGameId(),
                        new MoveRequest("e2", "e4", null),
                        INTRUDER_ID
                );
                case "resign" -> gameService.resign(session.getGameId(), INTRUDER_ID);
                case "offerDraw" -> gameService.offerDraw(session.getGameId(), INTRUDER_ID);
                case "acceptDraw" -> gameService.acceptDraw(session.getGameId(), INTRUDER_ID);
                case "declineDraw" -> gameService.declineDraw(session.getGameId(), INTRUDER_ID);
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
        });

        assertFalse(session.getGame().isGameOver(),
                "An unknown user must not cause timeout processing to mutate the game.");
        assertTrue(clockOf(session).isRunning(),
                "Clock should remain unchanged when the caller is not a participant.");
    }
}
