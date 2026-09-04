import org.junit.jupiter.api.Test;
import piece.Color;
import piece.Position;

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
}