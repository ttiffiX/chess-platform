package rules;

import model.*;
import model.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rules.services.CheckService;
import rules.services.DrawService;
import rules.services.LegalMoveService;
import rules.services.PathService;
import rules.validator.MoveValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameStatusEvaluatorTest {
    private GameStatusEvaluator evaluator;

    @BeforeEach
    void setUp() {
        PathService pathService = new PathService();
        CheckService checkService = new CheckService(pathService);
        MoveValidator moveValidator = new MoveValidator(pathService, checkService);
        LegalMoveService legalMoveService = new LegalMoveService(moveValidator);
        DrawService drawService = new DrawService();

        evaluator = new GameStatusEvaluator(legalMoveService, drawService);
    }

    private Board createCustomBoard(Map<Position, Piece> pieces) {
        return new Board(pieces);
    }

    @Test
    @DisplayName("Hòa do thiếu lực lượng: Vua vs Vua")
    void shouldDetectDrawInsufficientMaterialKingVsKing() {
        Map<Position, Piece> setup = new HashMap<>();
        setup.put(new Position('e', 1), new King(Color.WHITE));
        setup.put(new Position('e', 8), new King(Color.BLACK));
        Board board = createCustomBoard(setup);

        GameResult result = evaluator.evaluate(
                board, Color.WHITE, CastleRights.initial(), null, 0, Collections.emptyList(), false
        );

        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, result.status());
        assertNull(result.winner());
    }

    @Test
    @DisplayName("Hòa do thiếu lực lượng: Vua + Mã vs Vua")
    void shouldDetectDrawInsufficientMaterialKingKnightVsKing() {
        Map<Position, Piece> setup = new HashMap<>();
        setup.put(new Position('e', 1), new King(Color.WHITE));
        setup.put(new Position('g', 1), new Knight(Color.WHITE));
        setup.put(new Position('e', 8), new King(Color.BLACK));
        Board board = createCustomBoard(setup);

        GameResult result = evaluator.evaluate(
                board, Color.BLACK, CastleRights.initial(), null, 0, Collections.emptyList(), false
        );

        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, result.status());
        assertNull(result.winner());
    }

    @Test
    @DisplayName("Hòa Pat (Stalemate): Vua Đen không bị chiếu nhưng hết nước đi hợp lệ")
    void shouldDetectStalemate() {
        // Thế cờ Stalemate kinh điển: Vua Đen ở a8, Vua Trắng c7, Hậu Trắng b6
        Map<Position, Piece> setup = new HashMap<>();
        setup.put(new Position('a', 8), new King(Color.BLACK));
        setup.put(new Position('c', 7), new King(Color.WHITE));
        setup.put(new Position('b', 6), new Queen(Color.WHITE));
        Board board = createCustomBoard(setup);

        GameResult result = evaluator.evaluate(
                board, Color.BLACK, CastleRights.initial(), null, 0, Collections.emptyList(), false
        );

        assertEquals(GameStatus.STALEMATE, result.status());
        assertNull(result.winner());
    }

    @Test
    @DisplayName("Chiếu bí (Checkmate): Vua Đen bị chiếu và không còn nước đi thoát")
    void shouldDetectCheckmate() {
        // Thế cờ Hậu b7 chiếu Vua Đen ở a8, có Vua Trắng ở c7 bảo kê
        Map<Position, Piece> setup = new HashMap<>();
        setup.put(new Position('a', 8), new King(Color.BLACK));
        setup.put(new Position('c', 7), new King(Color.WHITE));
        setup.put(new Position('b', 7), new Queen(Color.WHITE));
        Board board = createCustomBoard(setup);

        // Đang bị chiếu (inCheck = true), tới lượt Đen
        GameResult result = evaluator.evaluate(
                board, Color.BLACK, CastleRights.initial(), null, 0, Collections.emptyList(), true
        );

        assertEquals(GameStatus.CHECKMATE, result.status());
        assertEquals(Color.WHITE, result.winner());
    }

    @Test
    @DisplayName("Hòa do luật 50 nước: halfMoveClock đạt 100")
    void shouldDetectDrawByFiftyMoveRule() {
        Map<Position, Piece> setup = new HashMap<>();
        setup.put(new Position('e', 1), new King(Color.WHITE));
        setup.put(new Position('d', 1), new Queen(Color.WHITE));
        setup.put(new Position('e', 8), new King(Color.BLACK));
        Board board = createCustomBoard(setup);

        GameResult result = evaluator.evaluate(
                board, Color.WHITE, CastleRights.initial(), null, 100, Collections.emptyList(), false
        );

        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, result.status());
        assertNull(result.winner());
    }

    @Test
    @DisplayName("Ván cờ tiếp diễn bình thường khi còn nước đi và không bị chiếu")
    void shouldReturnInProgressWhenGameIsActive() {
        Board board = new Board(); // Bàn cờ tiêu chuẩn lúc đầu

        GameResult result = evaluator.evaluate(
                board, Color.WHITE, CastleRights.initial(), null, 0, Collections.emptyList(), false
        );

        assertEquals(GameStatus.IN_PROGRESS, result.status());
        assertNull(result.winner());
    }
}