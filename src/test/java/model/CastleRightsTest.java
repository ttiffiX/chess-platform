package model;

import chess.engine.model.Position;
import chess.engine.model.game.CastleRights;
import org.junit.jupiter.api.Test;
import chess.engine.model.piece.Color;
import chess.engine.model.piece.King;
import chess.engine.model.piece.Queen;
import chess.engine.model.piece.Rook;

import static org.junit.jupiter.api.Assertions.*;

class CastleRightsTest {

    @Test
    void shouldCreateInitialCastleRightsWithAllSidesEnabled() {
        CastleRights castleRights = CastleRights.initial();

        assertTrue(castleRights.canCastleKingSide(Color.WHITE));
        assertTrue(castleRights.canCastleQueenSide(Color.WHITE));
        assertTrue(castleRights.canCastleKingSide(Color.BLACK));
        assertTrue(castleRights.canCastleQueenSide(Color.BLACK));
    }

    @Test
    void shouldDisableBothSidesWhenKingMoves() {
        CastleRights castleRights = CastleRights.initial().withMoveApplied(
                new King(Color.WHITE),
                new Position('e', 1),
                null,
                new Position('e', 2)
        );

        assertFalse(castleRights.canCastleKingSide(Color.WHITE));
        assertFalse(castleRights.canCastleQueenSide(Color.WHITE));
        assertTrue(castleRights.canCastleKingSide(Color.BLACK));
        assertTrue(castleRights.canCastleQueenSide(Color.BLACK));
    }

    @Test
    void shouldDisableOnlyMovedRookSide() {
        CastleRights castleRights = CastleRights.initial().withMoveApplied(
                new Rook(Color.BLACK),
                new Position('a', 8),
                null,
                new Position('a', 6)
        );

        assertTrue(castleRights.canCastleKingSide(Color.BLACK));
        assertFalse(castleRights.canCastleQueenSide(Color.BLACK));
        assertTrue(castleRights.canCastleKingSide(Color.WHITE));
        assertTrue(castleRights.canCastleQueenSide(Color.WHITE));
    }

    @Test
    void shouldDisableCapturedRookSide() {
        CastleRights castleRights = CastleRights.initial().withMoveApplied(
                new Queen(Color.WHITE),
                new Position('d', 1),
                new Rook(Color.BLACK),
                new Position('h', 8)
        );

        assertFalse(castleRights.canCastleKingSide(Color.BLACK));
        assertTrue(castleRights.canCastleQueenSide(Color.BLACK));
    }

    @Test
    void shouldRejectNullColorQuery() {
        CastleRights castleRights = CastleRights.initial();
        assertThrows(IllegalArgumentException.class, () -> castleRights.canCastleKingSide(null));
    }
}
