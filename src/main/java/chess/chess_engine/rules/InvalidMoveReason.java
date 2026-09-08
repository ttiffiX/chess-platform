package chess.chess_engine.rules;

public enum InvalidMoveReason {
    NO_PIECE_AT_SOURCE,
    WRONG_TURN,
    TARGET_HAS_ALLY_PIECE,
    ILLEGAL_PATTERN,
    PATH_BLOCKED,
    KING_LEFT_IN_CHECK,
    INVALID_CASTLING,
    INVALID_EN_PASSANT,
    INVALID_PROMOTION
    }
