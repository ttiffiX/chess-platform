import model.CastleRights;
import model.Position;
import org.junit.jupiter.api.Test;
import model.piece.Color;
import model.piece.King;
import model.piece.Queen;
import model.piece.Rook;

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
