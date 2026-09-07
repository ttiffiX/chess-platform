package model;

import model.piece.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    @Test
    @DisplayName("Mô phỏng ván cờ Opera Game kinh điển (Paul Morphy)")
    void testOperaGameSimulation() {
        Game game = new Game();

        // 1. e4 e5
        game.move(new Move(new Position('e', 2), new Position('e', 4)));
        game.move(new Move(new Position('e', 7), new Position('e', 5)));

        // 2. Nf3 d6
        game.move(new Move(new Position('g', 1), new Position('f', 3)));
        game.move(new Move(new Position('d', 7), new Position('d', 6)));

        // 3. d4 Bg4
        game.move(new Move(new Position('d', 2), new Position('d', 4)));
        game.move(new Move(new Position('c', 8), new Position('g', 4)));

        // 4. dxe5 Bxf3
        game.move(new Move(new Position('d', 4), new Position('e', 5)));
        game.move(new Move(new Position('g', 4), new Position('f', 3)));

        // 5. Qxf3 dxe5
        game.move(new Move(new Position('d', 1), new Position('f', 3)));
        game.move(new Move(new Position('d', 6), new Position('e', 5)));

        // 6. Bc4 Nf6
        game.move(new Move(new Position('f', 1), new Position('c', 4)));
        game.move(new Move(new Position('g', 8), new Position('f', 6)));

        // 7. Qb3 Qe7
        game.move(new Move(new Position('f', 3), new Position('b', 3)));
        game.move(new Move(new Position('d', 8), new Position('e', 7)));

        // Kiểm tra bàn cờ vẫn đang tiếp diễn mượt mà, không ngoại lệ
        assertEquals(GameStatus.IN_PROGRESS, game.getGameResult().status());
        assertFalse(game.isInCheck());
        assertEquals(Color.WHITE, game.getCurrentTurn());
    }
}