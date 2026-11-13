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
@Builder(toBuilder = true)
@Document(collection = "game_history")
@CompoundIndex(name = "player_recent", def = "{'playerIds': 1, 'endedAt': -1}")
public class GameHistory {
    @Id
    private String gameId;

    private int schemaVersion;  // = 1
    private String whitePlayerId;
    private String blackPlayerId;
    private List<String> playerIds;         // [white, black]
    private GameStatus status;
    private Color winner;                   // null nếu hòa
    private long baseSeconds;
    private long incrementSeconds;
    private String moves;                   // UCI, cách nhau bằng dấu cách
    private int plyCount;
    private long[] clocksMs;                 // null nếu không có đồng hồ
    private Instant startedAt;
    private Instant endedAt;
}