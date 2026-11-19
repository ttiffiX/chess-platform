package chess.feature.gamehistory.service;

import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.enums.GameOutcome;
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

import static chess.feature.gamehistory.enums.GameOutcome.*;

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
