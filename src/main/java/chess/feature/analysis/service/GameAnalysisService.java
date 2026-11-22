package chess.feature.analysis.service;

import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameResult;
import chess.feature.analysis.dto.GameReplayDto;
import chess.feature.analysis.dto.GameReplayRecordDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameAnalysisService {
    private final GameHistoryService gameHistoryService;

    public GameReplayDto getGameReplay(String gameId, String playerId) {
        // 1. Find history and verify that the requester participated.
        GameHistory history = gameHistoryService.getPlayerGame(gameId, playerId);

        // 2. Restore the game from its initial position.
        Game game = new Game();

        List<GameReplayRecordDto> records = new ArrayList<>();

        // Initial position (ply 0).
        records.add(createReplayRecord(
                game,
                null,
                null
        ));

        // 3. Replay all persisted UCI moves.
        String movesString = history.getMoves();
        List<String> moves = movesString.isBlank() ? List.of() : List.of(history.getMoves().trim().split("\\s+"));
        long[] clocksMs = history.getClocksMs();

        for (int i = 0; i < moves.size(); i++) {
            String uci = moves.get(i);

            Move move = Move.fromUCI(uci, game.getCurrentTurn());

            var result = game.move(move);

            if (!result.valid()) {
                throw new IllegalStateException(
                        "Cannot replay stored move " + uci + " at ply " + (i + 1) + ": " + result.message()
                );
            }

            Integer moverClockAfterMs = clocksMs == null ? null : Math.toIntExact(clocksMs[i]);

            records.add(createReplayRecord(
                    game,
                    uci,
                    moverClockAfterMs
            ));
        }

        // 4. Return the persisted game result, not the reconstructed one.
        GameResult persistedResult = new GameResult(
                history.getStatus(),
                history.getWinner()
        );

        return new GameReplayDto(
                history.getGameId(),
                history.getWhitePlayerId(),
                history.getBlackPlayerId(),
                persistedResult,
                history.getBaseSeconds(),
                history.getIncrementSeconds(),
                history.getPlyCount(),
                history.getStartedAt(),
                history.getEndedAt(),
                List.copyOf(records)
        );
    }

    private GameReplayRecordDto createReplayRecord(
            Game game,
            String move,
            Integer moverClockAfterMs
    ) {
        return new GameReplayRecordDto(
                move,
                game.getCurrentFEN(),
                game.getCapturedPiece(),
                game.isInCheck(),
                moverClockAfterMs,
                game.getLegalMovesMap()
        );
    }
}
