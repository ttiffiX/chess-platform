package chess.feature.gamehistory.entity;

import chess.engine.model.game.GameStatus;
import chess.engine.model.piece.Color;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@Document(collection = "game_history")
@CompoundIndex(name = "player_recent", def = "{'playerIds': 1, 'endedAt': -1}")
public class GameHistory {
    @Id
    @ToString.Include
    private String gameId;

    @ToString.Include
    private int schemaVersion;  // = 1

    @ToString.Include
    private String whitePlayerId;

    @ToString.Include
    private String blackPlayerId;

    private List<String> playerIds;         // [white, black]

    @ToString.Include
    private GameStatus status;

    @ToString.Include
    private Color winner;                   // null nếu hòa

    private long baseSeconds;
    private long incrementSeconds;
    private String moves;                   // UCI, cách nhau bằng dấu cách

    @ToString.Include
    private int plyCount;

    private long[] clocksMs;                 // null nếu không có đồng hồ

    @ToString.Include
    private Instant startedAt;

    @ToString.Include
    private Instant endedAt;
}