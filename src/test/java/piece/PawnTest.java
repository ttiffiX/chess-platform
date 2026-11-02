package piece;

import chess.engine.model.Position;
import chess.engine.model.piece.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pawn Tests")
class PawnTest {

    private final Pawn whitePawn = new Pawn(Color.WHITE);
    private final Pawn blackPawn = new Pawn(Color.BLACK);

    // =========================================================
    // MOVEMENT TESTS
    // =========================================================

    @Test
    @DisplayName("White pawn should move forward one or two squares from its starting position")
    void canMove_whitePawn_shouldAllowOneOrTwoSquaresFromStartingPosition() {
        Position from = new Position('e', 2);

        // White Pawn tiến về phía rank tăng.
        // Ở hàng xuất phát rank 2, nó được phép đi một hoặc hai ô.
        assertAll(
                () -> assertTrue(whitePawn.canMove(from, new Position('e', 3))),
                () -> assertTrue(whitePawn.canMove(from, new Position('e', 4)))
        );
    }

    @Test
    @DisplayName("Black pawn should move forward one or two squares from its starting position")
    void canMove_blackPawn_shouldAllowOneOrTwoSquaresFromStartingPosition() {
        Position from = new Position('e', 7);

        // Black Pawn đi ngược chiều với White, tức rank giảm.
        // Ở hàng xuất phát rank 7, nó được phép đi một hoặc hai ô.
        assertAll(
                () -> assertTrue(blackPawn.canMove(from, new Position('e', 6))),
                () -> assertTrue(blackPawn.canMove(from, new Position('e', 5)))
        );
    }

    @Test
    @DisplayName("Pawn should move forward one square from a non-starting position")
    void canMove_shouldAllowOneSquareFromNonStartingPosition() {
        // Sau khi rời vị trí ban đầu, Pawn chỉ được tiến một ô mỗi lượt.
        assertAll(
                () -> assertTrue(
                        whitePawn.canMove(
                                new Position('e', 3),
                                new Position('e', 4)
                        )
                ),
                () -> assertTrue(
                        blackPawn.canMove(
                                new Position('e', 6),
                                new Position('e', 5)
                        )
                )
        );
    }

    @Test
    @DisplayName("Pawn should not move two squares from a non-starting position")
    void canMove_shouldRejectTwoSquaresFromNonStartingPosition() {
        // Quyền đi hai ô chỉ áp dụng ở nước đi đầu tiên của Pawn.
        assertAll(
                () -> assertFalse(
                        whitePawn.canMove(
                                new Position('e', 3),
                                new Position('e', 5)
                        )
                ),
                () -> assertFalse(
                        blackPawn.canMove(
                                new Position('e', 6),
                                new Position('e', 4)
                        )
                )
        );
    }

    @Test
    @DisplayName("Pawn should reject backward, sideways and diagonal moves")
    void canMove_shouldRejectInvalidDirections() {
        Position whiteFrom = new Position('e', 4);
        Position blackFrom = new Position('e', 5);

        // Pawn không được tự do di chuyển như các quân khác:
        // - Không đi lùi.
        // - Không đi ngang.
        // - Không đi chéo bằng quy tắc di chuyển thông thường.
        assertAll(
                () -> assertFalse(whitePawn.canMove(whiteFrom, new Position('e', 3))),
                () -> assertFalse(whitePawn.canMove(whiteFrom, new Position('f', 4))),
                () -> assertFalse(whitePawn.canMove(whiteFrom, new Position('d', 5))),

                () -> assertFalse(blackPawn.canMove(blackFrom, new Position('e', 6))),
                () -> assertFalse(blackPawn.canMove(blackFrom, new Position('f', 5))),
                () -> assertFalse(blackPawn.canMove(blackFrom, new Position('d', 4)))
        );
    }

    @Test
    @DisplayName("Pawn should reject moving to the same position")
    void canMove_shouldRejectSamePosition() {
        Position from = new Position('e', 4);

        assertAll(
                () -> assertFalse(whitePawn.canMove(from, from)),
                () -> assertFalse(blackPawn.canMove(from, from))
        );
    }

    // =========================================================
    // CAPTURE TESTS
    // =========================================================

    @Test
    @DisplayName("White pawn should capture diagonally forward")
    void canCapture_whitePawn_shouldAllowDiagonalForwardCapture() {
        Position from = new Position('e', 4);

        // White Pawn bắt quân ở hai ô chéo phía trước:
        // trái trên và phải trên.
        assertAll(
                () -> assertTrue(whitePawn.canCapture(from, new Position('d', 5))),
                () -> assertTrue(whitePawn.canCapture(from, new Position('f', 5)))
        );
    }

    @Test
    @DisplayName("Black pawn should capture diagonally forward")
    void canCapture_blackPawn_shouldAllowDiagonalForwardCapture() {
        Position from = new Position('e', 5);

        // Black Pawn bắt quân theo hướng ngược lại:
        // hai ô chéo phía dưới.
        assertAll(
                () -> assertTrue(blackPawn.canCapture(from, new Position('d', 4))),
                () -> assertTrue(blackPawn.canCapture(from, new Position('f', 4)))
        );
    }

    @Test
    @DisplayName("Pawn should reject straight, backward and invalid captures")
    void canCapture_shouldRejectInvalidCaptureDirections() {
        Position whiteFrom = new Position('e', 4);
        Position blackFrom = new Position('e', 5);

        // Pawn không bắt quân bằng cách đi thẳng.
        // Nó cũng không được bắt chéo về phía sau hoặc đi ngang.
        assertAll(
                () -> assertFalse(whitePawn.canCapture(whiteFrom, new Position('e', 5))),
                () -> assertFalse(whitePawn.canCapture(whiteFrom, new Position('d', 3))),
                () -> assertFalse(whitePawn.canCapture(whiteFrom, new Position('f', 4))),

                () -> assertFalse(blackPawn.canCapture(blackFrom, new Position('e', 4))),
                () -> assertFalse(blackPawn.canCapture(blackFrom, new Position('d', 6))),
                () -> assertFalse(blackPawn.canCapture(blackFrom, new Position('f', 5)))
        );
    }

    // =========================================================
    // PIECE TYPE TEST
    // =========================================================

    @Test
    @DisplayName("Should return PAWN as its piece type")
    void getType_shouldReturnPawn() {
        assertAll(
                () -> assertEquals(PieceType.PAWN, whitePawn.getType()),
                () -> assertEquals(PieceType.PAWN, blackPawn.getType())
        );
    }

    // =========================================================
    // CANDIDATE DESTINATION TESTS
    // =========================================================

    @Test
    @DisplayName("White pawn should return forward and diagonal candidates from its starting position")
    void getCandidateDestinations_whitePawn_shouldReturnExpectedPositions() {
        Position from = new Position('e', 2);

        // Candidate của Pawn gồm:
        // - Các ô tiến thẳng có thể đi tới.
        // - Hai ô chéo mà Pawn có thể tấn công.
        //
        // Việc ô chéo có quân đối phương hay không sẽ được kiểm tra
        // ở tầng Board/MoveValidator, không thuộc method này.

        PieceTestAssertions.assertCandidates(
                whitePawn,
                from,
                new Position('e', 3),
                new Position('e', 4),
                new Position('d', 3),
                new Position('f', 3)
        );
    }

    @Test
    @DisplayName("Black pawn should return forward and diagonal candidates from its starting position")
    void getCandidateDestinations_blackPawn_shouldReturnExpectedPositions() {
        Position from = new Position('e', 7);

        PieceTestAssertions.assertCandidates(
                blackPawn,
                from,
                new Position('e', 6),
                new Position('e', 5),
                new Position('d', 6),
                new Position('f', 6)
        );
    }

    @Test
    @DisplayName("Pawn candidates should respect the left and right board edges")
    void getCandidateDestinations_shouldRespectBoardEdges() {
        // White Pawn ở a2 không có ô chéo bên trái.
        // Black Pawn ở h7 không có ô chéo bên phải.
        assertAll(
                () -> PieceTestAssertions.assertCandidates(
                        whitePawn,
                        new Position('a', 2),
                        new Position('a', 3),
                        new Position('a', 4),
                        new Position('b', 3)
                ),
                () -> PieceTestAssertions.assertCandidates(
                        blackPawn,
                        new Position('h', 7),
                        new Position('h', 6),
                        new Position('h', 5),
                        new Position('g', 6)
                )
        );
    }

    @Test
    @DisplayName("Pawn should be able to reach the promotion rank")
    void canMove_shouldAllowMovingToPromotionRank() {
        // Pawn được phép đi tới hàng cuối.
        // Việc đổi Pawn thành Queen, Rook, Bishop hoặc Knight
        // sẽ được xử lý ở tầng Game, không thuộc canMove().

        assertAll(
                () -> assertTrue(
                        whitePawn.canMove(
                                new Position('a', 7),
                                new Position('a', 8)
                        )
                ),
                () -> assertTrue(
                        blackPawn.canMove(
                                new Position('h', 2),
                                new Position('h', 1)
                        )
                )
        );
    }

    @Test
    @DisplayName("Pawn should not have two-square candidates after leaving its starting position")
    void getCandidateDestinations_shouldNotIncludeDoubleMoveFromNonStartingPosition() {
        // Quyền đi hai ô chỉ tồn tại ở nước đi đầu tiên.
        // Candidate sau khi Pawn đã di chuyển phải chỉ gồm một ô phía trước
        // và hai ô chéo có thể tấn công.

        PieceTestAssertions.assertCandidates(
                whitePawn,
                new Position('e', 4),
                new Position('e', 5),
                new Position('d', 5),
                new Position('f', 5)
        );

        PieceTestAssertions.assertCandidates(
                blackPawn,
                new Position('e', 5),
                new Position('e', 4),
                new Position('d', 4),
                new Position('f', 4)
        );
    }
}