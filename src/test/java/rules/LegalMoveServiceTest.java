package rules;

import chess.engine.model.Board;
import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.game.CastleRights;
import chess.engine.model.piece.*;
import chess.engine.rules.services.CheckService;
import chess.engine.rules.services.LegalMoveService;
import chess.engine.rules.services.PathService;
import chess.engine.rules.validator.MoveValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LegalMoveService Tests")
class LegalMoveServiceTest {

    private LegalMoveService legalMoveService;

    @BeforeEach
    void setUp() {
        PathService pathService = new PathService();
        CheckService checkService = new CheckService(pathService);
        MoveValidator moveValidator = new MoveValidator(pathService, checkService);

        legalMoveService = new LegalMoveService(moveValidator);
    }

    @Nested
    @DisplayName("Promotion Tests")
    class PromotionTests {

        @Test
        @DisplayName("Should generate all four promotion choices for white pawn")
        void shouldGenerateAllPromotionChoicesForWhitePawn() {
            Board board = createBoardWithPromotionPawn(Color.WHITE);

            List<Move> moves = legalMoveService.generateLegalMoves(
                    board,
                    Color.WHITE,
                    CastleRights.initial(),
                    null
            );

            List<Move> promotionMoves = getPromotionMoves(
                    moves,
                    new Position('a', 7),
                    new Position('a', 8)
            );

            assertEquals(4, promotionMoves.size());
            assertPromotionChoices(promotionMoves);
        }

        @Test
        @DisplayName("Should generate all four promotion choices for black pawn")
        void shouldGenerateAllPromotionChoicesForBlackPawn() {
            Board board = createBoardWithPromotionPawn(Color.BLACK);

            List<Move> moves = legalMoveService.generateLegalMoves(
                    board,
                    Color.BLACK,
                    CastleRights.initial(),
                    null
            );

            List<Move> promotionMoves = getPromotionMoves(
                    moves,
                    new Position('h', 2),
                    new Position('h', 1)
            );

            assertEquals(4, promotionMoves.size());
            assertPromotionChoices(promotionMoves);
        }

        @Test
        @DisplayName("Should recognize promotion as a legal move")
        void shouldRecognizePromotionAsLegalMove() {
            Board board = createBoardWithPromotionPawn(Color.WHITE);

            assertTrue(legalMoveService.hasAnyLegalMove(
                    board,
                    Color.WHITE,
                    CastleRights.initial(),
                    null
            ));
        }

        private Board createBoardWithPromotionPawn(Color color) {
            Position whiteKingPosition = new Position('e', 1);
            Position blackKingPosition = new Position('e', 8);

            Position pawnPosition = color == Color.WHITE
                    ? new Position('a', 7)
                    : new Position('h', 2);

            return new Board(Map.of(
                    whiteKingPosition, new King(Color.WHITE),
                    blackKingPosition, new King(Color.BLACK),
                    pawnPosition, new Pawn(color)
            ));
        }

        private List<Move> getPromotionMoves(
                List<Move> moves,
                Position from,
                Position to
        ) {
            return moves.stream()
                    .filter(move -> move.from().equals(from))
                    .filter(move -> move.to().equals(to))
                    .filter(move -> move.promotedPiece() != null)
                    .toList();
        }

        private void assertPromotionChoices(List<Move> moves) {
            assertTrue(moves.stream().anyMatch(m -> m.promotedPiece() instanceof Queen));
            assertTrue(moves.stream().anyMatch(m -> m.promotedPiece() instanceof Rook));
            assertTrue(moves.stream().anyMatch(m -> m.promotedPiece() instanceof Bishop));
            assertTrue(moves.stream().anyMatch(m -> m.promotedPiece() instanceof Knight));
        }
    }

    @Nested
    @DisplayName("Legal Move Consistency Tests")
    class LegalMoveConsistencyTests {

        @Test
        @DisplayName("hasAnyLegalMove should agree with generateLegalMoves")
        void shouldKeepLegalMoveMethodsConsistent() {
            Board board = new Board();

            List<Move> legalMoves = legalMoveService.generateLegalMoves(
                    board,
                    Color.WHITE,
                    CastleRights.initial(),
                    null
            );

            boolean hasAnyLegalMove = legalMoveService.hasAnyLegalMove(
                    board,
                    Color.WHITE,
                    CastleRights.initial(),
                    null
            );

            assertFalse(legalMoves.isEmpty());
            assertEquals(!legalMoves.isEmpty(), hasAnyLegalMove);
        }
    }
}