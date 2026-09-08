package piece;

import chess.engine.model.piece.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PieceTest {

    @Test
    void shouldCreateWhiteKing() {
        Piece king = new King(Color.WHITE);

        assertEquals(Color.WHITE, king.getColor());
    }

    @Test
    void shouldCreateBlackQueen() {
        Piece queen = new Queen(Color.BLACK);

        assertEquals(Color.BLACK, queen.getColor());
    }

    @Test
    void shouldCreateAllPieceTypes() {
        Piece king = new King(Color.WHITE);
        Piece queen = new Queen(Color.WHITE);
        Piece rook = new Rook(Color.WHITE);
        Piece bishop = new Bishop(Color.WHITE);
        Piece knight = new Knight(Color.WHITE);
        Piece pawn = new Pawn(Color.WHITE);

        assertEquals(Color.WHITE, king.getColor());
        assertEquals(Color.WHITE, queen.getColor());
        assertEquals(Color.WHITE, rook.getColor());
        assertEquals(Color.WHITE, bishop.getColor());
        assertEquals(Color.WHITE, knight.getColor());
        assertEquals(Color.WHITE, pawn.getColor());
    }

    @Test
    void shouldRejectNullColor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new King(null)
        );
    }

    @Test
    void allPiecesShouldBePiece() {
        assertInstanceOf(Piece.class, new King(Color.WHITE));
        assertInstanceOf(Piece.class, new Queen(Color.WHITE));
        assertInstanceOf(Piece.class, new Rook(Color.WHITE));
        assertInstanceOf(Piece.class, new Bishop(Color.WHITE));
        assertInstanceOf(Piece.class, new Knight(Color.WHITE));
        assertInstanceOf(Piece.class, new Pawn(Color.WHITE));
    }
}