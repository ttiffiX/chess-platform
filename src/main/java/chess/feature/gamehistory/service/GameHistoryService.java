package chess.feature.gamehistory.service;

import chess.engine.model.Move;
import chess.engine.model.game.Game;
import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.dto.GameReplayDto;
import chess.feature.gamehistory.dto.GameReplayRecordDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.model.GameOutcome;
import chess.feature.gamehistory.mapper.GameHistoryMapper;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import chess.feature.user.dto.PlayerSummary;
import chess.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static chess.feature.gamehistory.model.GameOutcome.*;

@Service
@RequiredArgsConstructor
public class GameHistoryService {

    private static final int HISTORY_PAGE_SIZE = 20;

    private static final Sort HISTORY_SORT =
            Sort.by(Sort.Direction.DESC, "endedAt");

    private final GameHistoryRepository gameHistoryRepository;
    private final GameHistoryMapper gameHistoryMapper;
    private final UserService userService;

    public void saveGameHistory(GameHistory gameHistory) {
        gameHistoryRepository.save(gameHistory);
    }

    public Page<GameHistoryDto> getGameHistory(
            String playerId,
            int page
    ) {
        if (page < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Page number should not be negative.");
        }

        // Pagination và sorting được kiểm soát hoàn toàn ở backend.
        Pageable pageable = PageRequest.of(
                page,
                HISTORY_PAGE_SIZE,
                HISTORY_SORT
        );

        // 1. Lấy lịch sử đấu của người chơi.
        Page<GameHistory> historyPage = gameHistoryRepository.findByPlayerIdsContaining(playerId, pageable);

        // 2. Thu thập ID đối thủ trên trang hiện tại.
        Set<String> opponentIds = new LinkedHashSet<>();

        for (GameHistory history : historyPage.getContent()) {
            opponentIds.add(resolveOpponentId(history, playerId));
        }

        // 3. Batch load thông tin đối thủ.
        Map<String, PlayerSummary> opponentMap = opponentIds.isEmpty() ? Map.of() :
                userService.getPlayerSummaries(new ArrayList<>(opponentIds))
                .stream().collect(Collectors.toMap(
                        PlayerSummary::playerId,
                        Function.identity()));

        // 4. Map entity thành DTO.
        // Page.map giữ nguyên metadata phân trang, kể cả khi trang rỗng.
        return historyPage.map(history -> {
            String opponentId = resolveOpponentId(history, playerId);

            PlayerSummary opponent = opponentMap.getOrDefault(
                    opponentId,
                    new PlayerSummary(
                            opponentId,
                            "Unknown player",
                            null
                    )
            );

            Color playerColor = resolvePlayerColor(history, playerId);

            GameOutcome outcome = resolveOutcome(
                    history.getWinner(),
                    playerColor
            );

            return gameHistoryMapper.toGameHistoryDto(
                    history,
                    opponent,
                    playerColor,
                    outcome
            );
        });
    }


    public GameReplayDto getGameReplay(String gameId, String playerId) {
        // 1. Find history and verify that the requester participated.
        GameHistory history = gameHistoryRepository.findById(gameId)
                .filter(item -> item.getPlayerIds() != null && item.getPlayerIds().contains(playerId))
                .orElseThrow(() -> new AppException(ErrorCode.GAME_NOT_FOUND, "Game history not found."));

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
        List<String> moves = List.of(history.getMoves().trim().split("\\s+"));
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

    private String resolveOpponentId(
            GameHistory history,
            String playerId
    ) {
        if (playerId.equals(history.getWhitePlayerId())) {
            return history.getBlackPlayerId();
        }

        if (playerId.equals(history.getBlackPlayerId())) {
            return history.getWhitePlayerId();
        }

        throw new IllegalStateException("Player is not a participant of game " + history.getGameId());
    }

    private Color resolvePlayerColor(
            GameHistory history,
            String playerId
    ) {
        if (playerId.equals(history.getWhitePlayerId())) {
            return Color.WHITE;
        }

        if (playerId.equals(history.getBlackPlayerId())) {
            return Color.BLACK;
        }

        throw new IllegalStateException(
                "Player is not a participant of game " + history.getGameId()
        );
    }

    private GameOutcome resolveOutcome(
            Color winner,
            Color playerColor
    ) {
        if (winner == null) {
            return DRAW;
        }

        return winner == playerColor
                ? WIN
                : LOSS;
    }
}
