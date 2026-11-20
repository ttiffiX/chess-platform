package chess.engine.model.game;

import chess.engine.model.Move;
import chess.engine.model.Position;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.PieceType;
import chess.engine.model.piece.Queen;
import chess.engine.utils.FenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game FEN Tests")
class GameFENTest {

    @Nested
    @DisplayName("FEN Generation Tests")
    class FENGenerationTests {

        @Nested
        @DisplayName("Initial FEN Tests")
        class InitialFENTests {

            @Test
            @DisplayName("Should generate correct FEN for initial position")
            void shouldGenerateInitialFEN() {
                Game game = new Game();

                assertEquals(
                        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                        game.getCurrentFEN()
                );
            }
        }

        @Nested
        @DisplayName("Piece Placement Tests")
        class PiecePlacementTests {

            @Test
            @DisplayName("Should generate correct piece placement after a pawn move")
            void shouldGeneratePiecePlacementAfterPawnMove() {
                Game game = new Game();

                play(game, "e2", "e4");

                assertEquals(
                        "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR",
                        game.getCurrentFEN().split(" ")[0]
                );
            }

            @Test
            @DisplayName("Should use uppercase for white pieces and lowercase for black pieces")
            void shouldUseCorrectPieceLetterCase() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "e7", "e5");

                String piecePlacement = game.getCurrentFEN().split(" ")[0];

                assertTrue(piecePlacement.contains("P"));
                assertTrue(piecePlacement.contains("p"));
            }
        }

        @Nested
        @DisplayName("Active Color Tests")
        class ActiveColorTests {

            @Test
            @DisplayName("Should indicate black to move after white moves")
            void shouldIndicateBlackToMove() {
                Game game = new Game();

                play(game, "e2", "e4");

                assertEquals("b", fenField(game, 1));
            }

            @Test
            @DisplayName("Should indicate white to move after black moves")
            void shouldIndicateWhiteToMove() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "e7", "e5");

                assertEquals("w", fenField(game, 1));
            }
        }

        @Nested
        @DisplayName("Castling Rights Tests")
        class CastlingRightsTests {

            @Test
            @DisplayName("Should include all castling rights in initial position")
            void shouldIncludeAllInitialCastlingRights() {
                Game game = new Game();

                assertEquals("KQkq", fenField(game, 2));
            }

            @Test
            @DisplayName("Should remove white castling rights after white king moves")
            void shouldRemoveWhiteCastlingRightsAfterKingMoves() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "a7", "a6");
                play(game, "e1", "e2");

                assertEquals("kq", fenField(game, 2));
            }

            @Test
            @DisplayName("Should remove corresponding castling right after rook moves")
            void shouldRemoveCorrespondingCastlingRightAfterRookMoves() {
                Game game = new Game();

                play(game, "h2", "h4");
                play(game, "a7", "a6");
                play(game, "h1", "h3");

                assertEquals("Qkq", fenField(game, 2));
            }
        }

        @Nested
        @DisplayName("En Passant Tests")
        class EnPassantTests {

            @Test
            @DisplayName("Should include en passant target after a two-square pawn move")
            void shouldIncludeEnPassantTargetAfterDoublePawnMove() {
                Game game = new Game();

                play(game, "e2", "e4");

                assertEquals("e3", fenField(game, 3));
            }

            @Test
            @DisplayName("Should include en passant target even when no capture is available")
            void shouldIncludeEnPassantTargetWithoutAdjacentPawn() {
                Game game = new Game();

                // No black pawn can capture on e3.
                play(game, "e2", "e4");

                assertEquals("e3", fenField(game, 3));
            }

            @Test
            @DisplayName("Should update en passant target after black double pawn move")
            void shouldUpdateEnPassantTargetAfterBlackDoublePawnMove() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "e7", "e5");

                assertEquals("e6", fenField(game, 3));
            }

            @Test
            @DisplayName("Should clear en passant target after the next move")
            void shouldClearEnPassantTargetAfterNextMove() {
                Game game = new Game();

                play(game, "e2", "e4");
                assertEquals("e3", fenField(game, 3));

                play(game, "a7", "a6");

                assertEquals("-", fenField(game, 3));
            }

            @Test
            @DisplayName("Should clear en passant target after an en passant capture")
            void shouldClearEnPassantTargetAfterCapture() {
                Game game = new Game();

                // 1. e4 a6 2. e5 d5
                play(game, "e2", "e4");
                play(game, "a7", "a6");
                play(game, "e4", "e5");
                play(game, "d7", "d5");

                assertEquals("d6", fenField(game, 3));

                // 3. exd6 e.p.
                play(game, "e5", "d6");

                assertEquals("-", fenField(game, 3));

                assertNotNull(game.getBoard().getPiece(position("d6")));
                assertNull(game.getBoard().getPiece(position("d5")));
            }
        }

        @Nested
        @DisplayName("Halfmove and Fullmove Tests")
        class MoveCounterTests {

            @Test
            @DisplayName("Should reset halfmove clock after a pawn move")
            void shouldResetHalfmoveClockAfterPawnMove() {
                Game game = new Game();

                play(game, "e2", "e4");

                assertEquals("0", fenField(game, 4));
            }

            @Test
            @DisplayName("Should increment halfmove clock after non-pawn moves")
            void shouldIncrementHalfmoveClockAfterNonPawnMoves() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "b8", "c6");
                play(game, "g1", "f3");

                assertEquals("2", fenField(game, 4));
            }

            @Test
            @DisplayName("Should reset halfmove clock after a capture")
            void shouldResetHalfmoveClockAfterCapture() {
                Game game = new Game();

                play(game, "e2", "e4");
                play(game, "d7", "d5");
                play(game, "e4", "d5");

                assertEquals("0", fenField(game, 4));
            }

            @Test
            @DisplayName("Should increment fullmove number after black moves")
            void shouldIncrementFullmoveNumberAfterBlackMoves() {
                Game game = new Game();

                assertEquals("1", fenField(game, 5));

                play(game, "e2", "e4");
                assertEquals("1", fenField(game, 5));

                play(game, "e7", "e5");
                assertEquals("2", fenField(game, 5));

                play(game, "g1", "f3");
                assertEquals("2", fenField(game, 5));
            }
        }

        @Nested
        @DisplayName("Promotion Tests")
        class PromotionTests {

            @Test
            @DisplayName("Should generate correct FEN after pawn promotion")
            void shouldGenerateCorrectFENAfterPromotion() {
                Game game = new Game();

                // Prepare a white pawn to capture the black rook on a8.
                play(game, "a2", "a4");
                play(game, "b7", "b5");
                play(game, "a4", "b5");
                play(game, "a7", "a6");
                play(game, "b5", "b6");
                play(game, "a6", "a5");
                play(game, "b6", "b7");
                play(game, "a5", "a4");

                Move promotionMove = new Move(
                        position("b7"),
                        position("a8"),
                        new Queen(Color.WHITE)
                );

                var result = game.move(promotionMove);

                assertTrue(result.valid());

                assertEquals(
                        "Qnbqkbnr/2pppppp/8/8/p7/8/1PPPPPPP/RNBQKBNR b KQk - 0 5",
                        game.getCurrentFEN()
                );
            }
        }

        private void play(Game game, String from, String to) {
            var result = game.move(move(from, to));

            assertTrue(
                    result.valid(),
                    () -> "Expected legal move: " + from + " -> " + to
                            + ", but got: " + result.reason()
            );
        }

        private String fenField(Game game, int index) {
            return game.getCurrentFEN().split(" ")[index];
        }

        private Move move(String from, String to) {
            return new Move(position(from), position(to));
        }

        private Position position(String square) {
            return new Position(
                    square.charAt(0),
                    Character.getNumericValue(square.charAt(1))
            );
        }
    }

    @Nested
    @DisplayName("FEN Parsing Tests")
    class FENParsingTests {

        private static final String INITIAL_FEN =
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        @Nested
        @DisplayName("Valid FEN Tests")
        class ValidFenTests {

            @Test
            @DisplayName("Should parse initial position correctly")
            void shouldParseInitialPosition() {
                FenState state = FenUtils.parseFen(INITIAL_FEN);

                assertEquals(Color.WHITE, state.currentTurn());
                assertEquals(CastleRights.initial(), state.castleRights());
                assertNull(state.enPassTarget());

                assertEquals(0, state.halfMoveClock());
                assertEquals(1, state.fullMoveNumber());

                assertEquals(32, state.board().getPieces().size());

                assertEquals(
                        PieceType.ROOK,
                        state.board().getPiece(position("a1")).getType()
                );

                assertEquals(
                        PieceType.KING,
                        state.board().getPiece(position("e1")).getType()
                );

                assertEquals(
                        PieceType.PAWN,
                        state.board().getPiece(position("e2")).getType()
                );

                assertEquals(
                        PieceType.KING,
                        state.board().getPiece(position("e8")).getType()
                );
            }

            @Test
            @DisplayName("Should parse active color, castling rights and counters")
            void shouldParseOtherFenFields() {
                String fen =
                        "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 17 23";

                FenState state = FenUtils.parseFen(fen);

                assertEquals(Color.BLACK, state.currentTurn());

                assertEquals(
                        new CastleRights(true, true, true, true),
                        state.castleRights()
                );

                assertNull(state.enPassTarget());
                assertEquals(17, state.halfMoveClock());
                assertEquals(23, state.fullMoveNumber());
                assertEquals(6, state.board().getPieces().size());
            }

            @Test
            @DisplayName("Should parse en passant target without adjacent pawn")
            void shouldParseEnPassantWithoutAdjacentPawn() {
                // Black has just moved e7-e5.
                // White has no pawn on d5 or f5 to capture en passant.
                String fen = "4k3/8/8/4p3/8/8/8/4K3 w - e6 0 2";

                FenState state = FenUtils.parseFen(fen);

                assertEquals(Color.WHITE, state.currentTurn());
                assertEquals(new Position('e', 6), state.enPassTarget());

                assertEquals(
                        PieceType.PAWN,
                        state.board().getPiece(position("e5")).getType()
                );

                assertEquals(0, state.halfMoveClock());
            }

            @Test
            @DisplayName("Should parse en passant target when capture is available")
            void shouldParseEnPassantWithAdjacentPawn() {
                String fen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2";

                FenState state = FenUtils.parseFen(fen);

                assertEquals(new Position('d', 6), state.enPassTarget());

                assertEquals(
                        PieceType.PAWN,
                        state.board().getPiece(position("d5")).getType()
                );

                assertEquals(
                        PieceType.PAWN,
                        state.board().getPiece(position("e5")).getType()
                );
            }
        }

        @Nested
        @DisplayName("Invalid FEN Tests")
        class InvalidFenTests {

            @Test
            @DisplayName("Should reject null or blank FEN")
            void shouldRejectNullOrBlankFen() {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FenUtils.parseFen(null)
                );

                assertThrows(
                        IllegalArgumentException.class,
                        () -> FenUtils.parseFen("")
                );

                assertThrows(
                        IllegalArgumentException.class,
                        () -> FenUtils.parseFen("   ")
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    // Wrong number of fields
                    "4k3/8/8/8/8/8/8/4K3 w - - 0",

                    // Invalid active color
                    "4k3/8/8/8/8/8/8/4K3 x - - 0 1",

                    // Wrong number of ranks
                    "4k3/8/8/8/8/8/4K3 w - - 0 1",

                    // Rank contains fewer than 8 squares
                    "4k3/8/8/8/8/8/8/4K2 w - - 0 1",

                    // Invalid piece symbol
                    "4k3/8/8/8/8/8/8/3XK3 w - - 0 1",

                    // Missing white king
                    "4k3/8/8/8/8/8/8/8 w - - 0 1",

                    // More than one white king
                    "4k3/8/8/8/8/8/8/3KK3 w - - 0 1",

                    // Pawn on rank 8
                    "P3k3/8/8/8/8/8/8/4K3 w - - 0 1",

                    // Duplicate castling right
                    "4k3/8/8/8/8/8/8/R3K2R w KK - 0 1",

                    // Castling right without corresponding rook
                    "4k3/8/8/8/8/8/8/4K3 w K - 0 1",

                    // Invalid en passant target notation
                    "4k3/8/8/4p3/8/8/8/4K3 w - z6 0 2",

                    // Invalid en passant target rank for active color
                    "4k3/8/8/4p3/8/8/8/4K3 b - e6 0 2",

                    // En passant target square is occupied
                    "4k3/8/4N3/4p3/8/8/8/4K3 w - e6 0 2",

                    // No pawn behind en passant target
                    "4k3/8/8/8/8/8/8/4K3 w - e6 0 2",

                    // Pawn's original square is occupied
                    "4k3/4n3/8/4p3/8/8/8/4K3 w - e6 0 2",

                    // Negative halfmove clock
                    "4k3/8/8/8/8/8/8/4K3 w - - -1 1",

                    // Fullmove number must be at least 1
                    "4k3/8/8/8/8/8/8/4K3 w - - 0 0"
            })
            @DisplayName("Should reject malformed or inconsistent FEN")
            void shouldRejectInvalidFen(String fen) {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> FenUtils.parseFen(fen),
                        () -> "Expected invalid FEN to be rejected: " + fen
                );
            }
        }

        private static Position position(String square) {
            return new Position(
                    square.charAt(0),
                    Character.getNumericValue(square.charAt(1))
            );
        }

        private static final String AFTER_E4 =
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";

        @Nested
        @DisplayName("Initialization Tests")
        class InitializationTests {

            @Test
            @DisplayName("Should initialize game correctly from FEN")
            void shouldInitializeFromFen() {
                Game game = new Game(FenUtils.parseFen(AFTER_E4));

                assertEquals(AFTER_E4, game.getCurrentFEN());

                assertEquals(Color.BLACK, game.getCurrentTurn());
                assertEquals(CastleRights.initial(), game.getCastleRights());

                assertEquals(0, game.getHalfMoveClock());
                assertEquals(1, game.getFullMoveNumber());

                assertFalse(game.isInCheck());
                assertFalse(game.isGameOver());

                assertEquals(
                        GameStatus.IN_PROGRESS,
                        game.getGameResult().status()
                );

                assertNull(game.getCapturedPiece());

                assertEquals(32, game.getBoard().getPieces().size());

                assertEquals(
                        PieceType.PAWN,
                        game.getBoard().getPiece(position("e4")).getType()
                );

                assertEquals(20, game.getAllLegalMovesByColor().size());
            }

            @Test
            @DisplayName("Should initialize legal moves from loaded position")
            void shouldGenerateLegalMovesFromLoadedPosition() {
                Game game = new Game(FenUtils.parseFen(AFTER_E4));

                assertTrue(
                        game.getAllLegalMovesByColor().stream()
                                .anyMatch(move ->
                                        move.from().equals(position("e7"))
                                                && move.to().equals(position("e5"))
                                )
                );
            }

            @Test
            @DisplayName("Should not generate en passant move without adjacent pawn")
            void shouldNotGenerateEnPassantWithoutAdjacentPawn() {
                String fen = "4k3/8/8/4p3/8/8/8/4K3 w - e6 0 2";

                Game game = new Game(FenUtils.parseFen(fen));

                assertEquals("e6", game.getCurrentFEN().split(" ")[3]);

                assertTrue(
                        game.getAllLegalMovesByColor().stream()
                                .noneMatch(move -> move.to().equals(position("e6")))
                );
            }

            @Test
            @DisplayName("Should initialize a checkmate position correctly")
            void shouldInitializeCheckmatePosition() {
                String fen = "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1";

                Game game = new Game(FenUtils.parseFen(fen));

                assertTrue(game.isInCheck());
                assertTrue(game.isGameOver());

                assertEquals(
                        GameStatus.CHECKMATE,
                        game.getGameResult().status()
                );

                assertEquals(Color.WHITE, game.getGameResult().winner());

                assertTrue(game.getAllLegalMovesByColor().isEmpty());
            }

            @Test
            @DisplayName("Should reject invalid FEN when creating game")
            void shouldRejectInvalidFen() {
                String invalidFen = "invalid-fen";

                assertThrows(
                        IllegalArgumentException.class,
                        () -> new Game(FenUtils.parseFen(invalidFen))
                );
            }
        }

        @Nested
        @DisplayName("Move Lifecycle Tests")
        class MoveLifecycleTests {

            @Test
            @DisplayName("Should continue playing from loaded FEN")
            void shouldContinuePlayingFromLoadedFen() {
                Game game = new Game(FenUtils.parseFen(AFTER_E4));

                var result = game.move(move("e7", "e5"));

                assertTrue(
                        result.valid(),
                        () -> "Expected e7-e5 to be legal, got: " + result.reason()
                );

                assertEquals(
                        "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                        game.getCurrentFEN()
                );

                assertEquals(Color.WHITE, game.getCurrentTurn());
                assertEquals(2, game.getFullMoveNumber());
            }

            @Test
            @DisplayName("Should preserve en passant state after loading FEN")
            void shouldPreserveEnPassantStateAfterLoading() {
                Game game = new Game(FenUtils.parseFen(AFTER_E4));

                assertEquals("e3", game.getCurrentFEN().split(" ")[3]);

                // Black plays a normal move.
                assertTrue(game.move(move("a7", "a6")).valid());

                // The previous en passant target must be cleared.
                assertEquals("-", game.getCurrentFEN().split(" ")[3]);
            }
        }

        private static Move move(String from, String to) {
            return new Move(position(from), position(to));
        }
    }
}