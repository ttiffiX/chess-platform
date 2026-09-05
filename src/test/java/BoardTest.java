import model.Board;
import model.Move;
import model.Position;
import model.piece.Color;
import model.piece.Knight;
import model.piece.Pawn;
import model.piece.Piece;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void shouldMovePieceToNewPosition() {
        Board board = new Board();

        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        board.movePiece(new Move(from, to));

        assertNull(board.getPiece(from));
        assertNotNull(board.getPiece(to));
        assertTrue(board.getPiece(to) instanceof Pawn);
        assertEquals(Color.WHITE, board.getPiece(to).getColor());
    }

    @Test
    void shouldPreservePieceIdentityWhenMoving() {
        Board board = new Board();

        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        Piece pawn = board.getPiece(from);

        board.movePiece(new Move(from, to));

        assertSame(pawn, board.getPiece(to));
        assertNull(board.getPiece(from));
    }

    @Test
    void shouldRejectNullMove() {
        Board board = new Board();

        assertThrows(
                IllegalArgumentException.class,
                () -> board.movePiece(null)
        );
    }

    @Test
    void shouldRejectMoveFromEmptyPosition() {
        Board board = new Board();

        Position from = new Position('e', 4);
        Position to = new Position('e', 5);

        assertNull(board.getPiece(from));

        assertThrows(
                IllegalArgumentException.class,
                () -> board.movePiece(new Move(from, to))
        );
    }

    @Test
    void shouldCapturePieceAtDestination() {
        Board board = new Board();

        Position whitePawnPosition = new Position('e', 2);
        Position blackPawnPosition = new Position('e', 7);

        Piece whitePawn = board.getPiece(whitePawnPosition);

        // model.Move white pawn to e7 directly.
        // Movement legality is NOT model.Board's responsibility yet.
        board.movePiece(
                new Move(whitePawnPosition, blackPawnPosition)
        );

        assertNull(board.getPiece(whitePawnPosition));
        assertSame(whitePawn, board.getPiece(blackPawnPosition));
        assertEquals(Color.WHITE, board.getPiece(blackPawnPosition).getColor());
    }

    @Test
    void shouldMoveDifferentPieceTypes() {
        Board board = new Board();

        Position from = new Position('b', 1);
        Position to = new Position('c', 3);

        Piece knight = board.getPiece(from);

        board.movePiece(new Move(from, to));

        assertNull(board.getPiece(from));
        assertSame(knight, board.getPiece(to));
        assertTrue(board.getPiece(to) instanceof Knight);
    }

    @Test
    void shouldNotAffectOtherPiecesWhenMoving() {
        Board board = new Board();

        Position from = new Position('e', 2);
        Position to = new Position('e', 4);

        Piece originalQueen = board.getPiece(
                new Position('d', 1)
        );

        Piece originalKing = board.getPiece(
                new Position('e', 1)
        );

        board.movePiece(new Move(from, to));

        assertSame(
                originalQueen,
                board.getPiece(new Position('d', 1))
        );

        assertSame(
                originalKing,
                board.getPiece(new Position('e', 1))
        );
    }
}