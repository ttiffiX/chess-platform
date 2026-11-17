package chess.feature.gamehistory.service;

import chess.engine.model.piece.Color;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.enums.GameOutcome;
import chess.feature.gamehistory.mapper.GameHistoryMapper;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import chess.feature.user.dto.PlayerSummary;
import chess.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static chess.feature.gamehistory.enums.GameOutcome.*;

@Service
@RequiredArgsConstructor
public class GameHistoryService {
    private final GameHistoryRepository gameHistoryRepository;
    private final GameHistoryMapper gameHistoryMapper;
    private final UserService userService;

    public void saveGameHistory(GameHistory gameHistory) {
        gameHistoryRepository.save(gameHistory);
    }

    public Page<GameHistoryDto> getGameHistory(
            String playerId,
            Pageable pageable
    ) {
        Page<GameHistory> historyPage = gameHistoryRepository.findByPlayerIdsContaining(playerId, pageable);

        if (historyPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<String> opponentIds = new ArrayList<>();

        for (GameHistory history : historyPage.getContent()) {
            opponentIds.add(resolveOpponentId(history.getPlayerIds(), playerId));
        }

        Map<String, PlayerSummary> opponentMap = userService.getPlayerSummaries(opponentIds)
                .stream()
                .collect(Collectors.toMap(PlayerSummary::playerId, Function.identity()));

        return historyPage.map(history -> {
            String opponentId = resolveOpponentId(history.getPlayerIds(), playerId);

            PlayerSummary opponent = opponentMap.get(opponentId);

            Color playerColor = resolvePlayerColor(history, playerId);
            GameOutcome outcome = resolveOutcome(history.getWinner(), playerColor);

            return gameHistoryMapper.toGameHistoryDto(history, opponent, playerColor, outcome);
        });
    }

    private String resolveOpponentId(List<String> playerIds, String playerId) {
        playerIds.remove(playerId);
        return playerIds.getFirst();
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
