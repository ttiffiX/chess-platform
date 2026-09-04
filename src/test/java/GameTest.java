import model.Game;
import model.Move;
import model.CastleRights;
import org.junit.jupiter.api.Test;
import piece.Color;
import model.Position;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    void shouldStartWithWhiteTurn() {
        Game game = new Game();

        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void shouldChangeToBlackAfterWhiteMove() {
        Game game = new Game();

        game.move(new Move(
                new Position('e', 2),
                new Position('e', 4)
        ));

        assertEquals(Color.BLACK, game.getCurrentTurn());
    }

    @Test
    void shouldChangeBackToWhiteAfterBlackMove() {
        Game game = new Game();

        game.move(new Move(
                new Position('e', 2),
                new Position('e', 4)
        ));

        game.move(new Move(
                new Position('e', 7),
                new Position('e', 5)
        ));

        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void shouldRejectMoveWhenItIsNotPlayersTurn() {
        Game game = new Game();

        assertThrows(
                IllegalArgumentException.class,
                () -> game.move(new Move(
                        new Position('e', 7),
                        new Position('e', 5)
                ))
        );

        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void shouldNotChangeTurnWhenMoveIsInvalid() {
        Game game = new Game();

        assertThrows(
                IllegalArgumentException.class,
                () -> game.move(new Move(
                        new Position('e', 2),
                        new Position('e', 5)
                ))
        );

        assertEquals(Color.WHITE, game.getCurrentTurn());
    }

    @Test
    void shouldMovePieceOnBoard() {
        Game game = new Game();

        game.move(new Move(
                new Position('e', 2),
                new Position('e', 4)
        ));

        assertNull(
                game.getBoard().getPiece(new Position('e', 2))
        );

        assertNotNull(
                game.getBoard().getPiece(new Position('e', 4))
        );

        assertEquals(
                Color.WHITE,
                game.getBoard()
                        .getPiece(new Position('e', 4))
                        .getColor()
        );
    }

    @Test
    void shouldStartWithFullCastleRights() {
        Game game = new Game();

        CastleRights castleRights = game.getCastleRights();

        assertTrue(castleRights.whiteKingSide());
        assertTrue(castleRights.whiteQueenSide());
        assertTrue(castleRights.blackKingSide());
        assertTrue(castleRights.blackQueenSide());
    }

    @Test
    void shouldLoseWhiteKingSideCastlingRightAfterWhiteRookMoves() {
        Game game = new Game();

        game.move(new Move(
                new Position('h', 2),
                new Position('h', 4)
        ));

        game.move(new Move(
                new Position('a', 7),
                new Position('a', 6)
        ));

        game.move(new Move(
                new Position('h', 1),
                new Position('h', 3)
        ));

        CastleRights castleRights = game.getCastleRights();

        assertFalse(castleRights.whiteKingSide());
        assertTrue(castleRights.whiteQueenSide());
        assertTrue(castleRights.blackKingSide());
        assertTrue(castleRights.blackQueenSide());
    }

    @Test
    void shouldLoseBothWhiteCastlingRightsAfterWhiteKingMoves() {
        Game game = new Game();

        game.move(new Move(
                new Position('e', 2),
                new Position('e', 4)
        ));

        game.move(new Move(
                new Position('a', 7),
                new Position('a', 6)
        ));

        game.move(new Move(
                new Position('e', 1),
                new Position('e', 2)
        ));

        CastleRights castleRights = game.getCastleRights();

        assertFalse(castleRights.whiteKingSide());
        assertFalse(castleRights.whiteQueenSide());
        assertTrue(castleRights.blackKingSide());
        assertTrue(castleRights.blackQueenSide());
    }
}