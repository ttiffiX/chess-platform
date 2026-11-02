package chess.feature.matchmaking.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchmakingTimeControlPreset {

    UNLIMITED(0, 0),

    BULLET_1_0(60, 0),
    BULLET_2_1(120, 1),

    BLITZ_3_0(180, 0),
    BLITZ_3_2(180, 2),
    BLITZ_5_0(300, 0),
    BLITZ_5_3(300, 3),

    RAPID_10_0(600, 0),
    RAPID_10_5(600, 5),
    RAPID_15_10(900, 10),

    CLASSICAL_30_0(1800, 0);

    private final long baseTimeSeconds;
    private final long incrementSeconds;
}