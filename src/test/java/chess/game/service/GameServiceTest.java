package chess.game.service;

import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.dto.request.MoveRequest;
import chess.feature.game.dto.response.GameResponse;
import chess.feature.game.model.Record;
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

    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String AFTER_E4_FEN =
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";

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

    private GameSession createGame(String player1Id, String player2Id) {
        return gameService.createGame(new CreateGameRequest(
                0L,
                0L,
                player1Id,
                player2Id
        ));
    }

    /**
     * Chỉ truy cập clock trực tiếp trong các test cần chủ động điều khiển thời gian.
     */
    private ChessClock clockOf(GameSession session) {
        Object clock = ReflectionTestUtils.getField(session, "clock");

        assertNotNull(clock, "GameSession should contain a ChessClock field named 'clock'.");

        return (ChessClock) clock;
    }

    private void forceClockExpired(GameSession session) {
        ReflectionTestUtils.setField(
                clockOf(session),
                "lastTimestamp",
                Instant.now().minusSeconds(BASE_TIME_SECONDS + 60)
        );
    }

    private void makeMove(
            GameSession session,
            String playerId,
            String from,
            String to
    ) {
        gameService.makeMove(
                session.getGameId(),
                new MoveRequest(from, to, null),
                playerId
        );
    }

    // -------------------------------------------------------------------------
    // Create game
    // -------------------------------------------------------------------------

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
    @DisplayName("Create game: đăng ký cả hai người chơi là đang trong ván")
    void createGame_RegistersBothPlayersAsActive() {
        GameSession session = createGame("player-1", "player-2");

        assertNotNull(session);
        assertTrue(gameService.hasActiveGame("player-1"));
        assertTrue(gameService.hasActiveGame("player-2"));
        assertFalse(gameService.hasActiveGame("player-3"));
    }

    @Test
    @DisplayName("Create game: từ chối người chơi đã có ván đang hoạt động")
    void createGame_PlayerAlreadyInGame_ThrowsException() {
        createGame("player-1", "player-2");

        assertThrows(
                AppException.class,
                () -> createGame("player-1", "player-3")
        );

        assertTrue(gameService.hasActiveGame("player-1"));
        assertTrue(gameService.hasActiveGame("player-2"));
        assertFalse(gameService.hasActiveGame("player-3"));
    }

    @Test
    @DisplayName("Has active game: trả false khi người chơi chưa tham gia ván nào")
    void hasActiveGame_NoActiveGame_ReturnsFalse() {
        assertFalse(gameService.hasActiveGame("player-1"));
    }

    // -------------------------------------------------------------------------
    // Get game
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Get game: người chơi hợp lệ lấy được trạng thái và lịch sử ban đầu")
    void getGame_PlayerInGame_ReturnsGameAndInitialHistory() {
        GameSession session = createTimedGame();

        GameResponse response = gameService.getGame(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertEquals(session.getGameId(), response.gameId());

        assertEquals(INITIAL_FEN, response.fen());

        assertNotNull(response.historyItems());
        assertEquals(1, response.historyItems().size());

        Record initialRecord = response.historyItems().getFirst();

        assertNull(initialRecord.move());
        assertEquals(INITIAL_FEN, initialRecord.fen());
        assertNull(initialRecord.capturedPiece());
        assertFalse(initialRecord.inCheck());
        assertNull(initialRecord.moverClockAfterMs());
    }

    @Test
    @DisplayName("Get game: trả về toàn bộ lịch sử sau nhiều nước đi")
    void getGame_AfterSeveralMoves_ReturnsFullHistory() {
        GameSession session = createTimedGame();

        String whitePlayerId = session.getWhitePlayerId();
        String blackPlayerId = session.getBlackPlayerId();

        makeMove(session, whitePlayerId, "e2", "e4");
        makeMove(session, blackPlayerId, "e7", "e5");

        GameResponse response = gameService.getGame(
                session.getGameId(),
                whitePlayerId
        );

        assertNotNull(response);
        assertEquals(session.getGameId(), response.gameId());

        // Bao gồm record khởi tạo và hai nước đi.
        assertEquals(3, response.historyItems().size());

        assertNull(response.historyItems().get(0).move());

        Record whiteMove = response.historyItems().get(1);
        assertEquals("e2", whiteMove.move().from().toString());
        assertEquals("e4", whiteMove.move().to().toString());
        assertEquals(AFTER_E4_FEN, whiteMove.fen());

        Record blackMove = response.historyItems().get(2);
        assertEquals("e7", blackMove.move().from().toString());
        assertEquals("e5", blackMove.move().to().toString());

        assertEquals(blackMove.fen(), response.fen());
        assertEquals(session.getFEN(), response.fen());
    }

    @Test
    @DisplayName("Get game: người ngoài ván bị từ chối")
    void getGame_UnknownPlayer_ThrowsException() {
        GameSession session = createTimedGame();

        assertThrows(AppException.class, () ->
                gameService.getGame(session.getGameId(), INTRUDER_ID));
    }

    // -------------------------------------------------------------------------
    // Make move
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Make move: nước đi hợp lệ cập nhật bàn cờ, lượt và clock")
    void makeMove_ValidMove_UpdatesGameAndClock() {
        GameSession session = createTimedGame();
        String whiteId = session.getWhitePlayerId();

        GameResponse response = gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e2", "e4", null),
                whiteId
        );

        assertNotNull(response);
        assertEquals(AFTER_E4_FEN, response.fen());

        // Response của action chỉ chứa history record mới nhất.
        assertEquals(1, response.historyItems().size());

        Record latestRecord = response.historyItems().getFirst();
        assertEquals("e2", latestRecord.move().from().toString());
        assertEquals("e4", latestRecord.move().to().toString());
        assertEquals(AFTER_E4_FEN, latestRecord.fen());

        assertEquals(Color.BLACK, session.getGame().getCurrentTurn());
        assertEquals(Color.BLACK, clockOf(session).getActiveColor());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Make move: nước đi không hợp lệ không được áp dụng hoặc ghi vào history")
    void makeMove_InvalidMove_LeavesGameAndHistoryUnchanged() {
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

        assertEquals(1, session.getHistory().size());
        assertEquals(INITIAL_FEN, session.getFEN());
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
        assertEquals(1, session.getHistory().size());
    }

    @Test
    @DisplayName("Make move: timeout được xử lý trước khi áp dụng nước đi")
    void makeMove_ClockExpired_EndsGameWithoutApplyingMove() {
        GameSession session = createTimedGame();
        forceClockExpired(session);

        GameResponse response = gameService.makeMove(
                session.getGameId(),
                new MoveRequest("e2", "e4", null),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());

        assertEquals(Color.WHITE, session.getGame().getCurrentTurn());
        assertEquals(INITIAL_FEN, session.getFEN());
        assertEquals(1, session.getHistory().size());

        assertEquals(GameStatus.TIMED_OUT, response.gameResult().status());
    }

    // -------------------------------------------------------------------------
    // Resign
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Resign: kết thúc ván, dừng clock và xóa draw offer")
    void resign_ValidPlayer_EndsGameAndStopsClock() {
        GameSession session = createTimedGame();
        session.offerDraw(Color.WHITE);

        GameResponse response = gameService.resign(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertNull(session.getDrawOfferedBy());

        assertEquals(GameStatus.RESIGNED, response.gameResult().status());
    }

    @Test
    @DisplayName("Resign: giải phóng cả hai người chơi khỏi active game")
    void resign_ReleasesBothPlayers() {
        GameSession session = createGame("player-1", "player-2");

        String whitePlayerId = session.getWhitePlayerId();
        String blackPlayerId = session.getBlackPlayerId();

        gameService.resign(session.getGameId(), whitePlayerId);

        assertTrue(session.getGame().isGameOver());
        assertFalse(gameService.hasActiveGame(whitePlayerId));
        assertFalse(gameService.hasActiveGame(blackPlayerId));
    }

    // -------------------------------------------------------------------------
    // Draw offer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Offer draw: lưu người đề nghị nhưng ván vẫn tiếp tục")
    void offerDraw_ValidPlayer_StoresOfferWithoutEndingGame() {
        GameSession session = createTimedGame();

        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertEquals(Color.WHITE, session.getDrawOfferedBy());
        assertFalse(session.getGame().isGameOver());
        assertTrue(clockOf(session).isRunning());
    }

    @Test
    @DisplayName("Offer draw: không cho phép có hai đề nghị hòa đang chờ")
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

        GameResponse response = gameService.acceptDraw(
                session.getGameId(),
                session.getBlackPlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());
        assertNull(session.getDrawOfferedBy());

        assertEquals(GameStatus.DRAW_BY_AGREEMENT, response.gameResult().status());
    }

    @Test
    @DisplayName("Accept draw: người đề nghị không thể tự chấp nhận")
    void acceptDraw_OwnOffer_ThrowsException() {
        GameSession session = createTimedGame();

        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertThrows(AppException.class, () ->
                gameService.acceptDraw(session.getGameId(), session.getWhitePlayerId()));

        assertFalse(session.getGame().isGameOver());
        assertEquals(Color.WHITE, session.getDrawOfferedBy());
    }

    @Test
    @DisplayName("Accept draw: giải phóng cả hai người chơi khỏi active game")
    void acceptDraw_ReleasesBothPlayers() {
        GameSession session = createGame("player-1", "player-2");

        String whitePlayerId = session.getWhitePlayerId();
        String blackPlayerId = session.getBlackPlayerId();

        gameService.offerDraw(session.getGameId(), whitePlayerId);
        gameService.acceptDraw(session.getGameId(), blackPlayerId);

        assertTrue(session.getGame().isGameOver());
        assertFalse(gameService.hasActiveGame(whitePlayerId));
        assertFalse(gameService.hasActiveGame(blackPlayerId));
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
    @DisplayName("Decline draw: người đề nghị không thể tự từ chối đề nghị của mình")
    void declineDraw_OwnOffer_ThrowsException() {
        GameSession session = createTimedGame();

        gameService.offerDraw(session.getGameId(), session.getWhitePlayerId());

        assertThrows(AppException.class, () ->
                gameService.declineDraw(session.getGameId(), session.getWhitePlayerId()));

        assertEquals(Color.WHITE, session.getDrawOfferedBy());
    }

    // -------------------------------------------------------------------------
    // Timeout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Claim timeout: trước khi hết giờ không kết thúc ván")
    void claimTimeout_NotExpired_KeepsGameInProgress() {
        GameSession session = createTimedGame();

        GameResponse response = gameService.claimTimeout(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertFalse(session.getGame().isGameOver());
        assertTrue(clockOf(session).isRunning());

        assertEquals(GameStatus.IN_PROGRESS, response.gameResult().status());
    }

    @Test
    @DisplayName("Claim timeout: khi hết giờ kết thúc ván và dừng clock")
    void claimTimeout_Expired_EndsGameAndStopsClock() {
        GameSession session = createTimedGame();
        forceClockExpired(session);

        GameResponse response = gameService.claimTimeout(
                session.getGameId(),
                session.getWhitePlayerId()
        );

        assertNotNull(response);
        assertTrue(session.getGame().isGameOver());
        assertFalse(clockOf(session).isRunning());

        assertEquals(GameStatus.TIMED_OUT, response.gameResult().status());
        assertFalse(gameService.hasActiveGame(session.getWhitePlayerId()));
        assertFalse(gameService.hasActiveGame(session.getBlackPlayerId()));
    }

    // -------------------------------------------------------------------------
    // Player validation / timeout regression
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Unknown player cannot trigger timeout through {0}")
    @ValueSource(strings = {
            "makeMove",
            "resign",
            "offerDraw",
            "acceptDraw",
            "declineDraw"
    })
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

        assertFalse(
                session.getGame().isGameOver(),
                "An unknown user must not cause timeout processing to mutate the game."
        );

        assertTrue(
                clockOf(session).isRunning(),
                "Clock should remain unchanged when the caller is not a participant."
        );
    }

    // -------------------------------------------------------------------------
    // Active game lifecycle
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Make move: giải phóng người chơi sau khi kết thúc bằng checkmate")
    void makeMove_ReleasesPlayersAfterCheckmate() {
        GameSession session = createGame("player-1", "player-2");

        String whitePlayerId = session.getWhitePlayerId();
        String blackPlayerId = session.getBlackPlayerId();

        // Fool's Mate:
        // 1. f3 e5
        // 2. g4 Qh4#
        makeMove(session, whitePlayerId, "f2", "f3");
        makeMove(session, blackPlayerId, "e7", "e5");
        makeMove(session, whitePlayerId, "g2", "g4");
        makeMove(session, blackPlayerId, "d8", "h4");

        assertTrue(session.getGame().isGameOver());
        assertEquals(GameStatus.CHECKMATE, session.getGame().getGameResult().status());

        assertFalse(gameService.hasActiveGame(whitePlayerId));
        assertFalse(gameService.hasActiveGame(blackPlayerId));

        // Hai người có thể tạo game mới.
        GameSession newSession = gameService.createGame(
                new CreateGameRequest(
                        0L,
                        0L,
                        whitePlayerId,
                        blackPlayerId
                )
        );

        assertNotNull(newSession);
        assertTrue(gameService.hasActiveGame(whitePlayerId));
        assertTrue(gameService.hasActiveGame(blackPlayerId));
    }
}