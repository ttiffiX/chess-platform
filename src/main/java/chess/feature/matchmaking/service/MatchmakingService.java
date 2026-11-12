package chess.feature.matchmaking.service;

import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.model.TimeControl;
import chess.feature.game.service.GameService;
import chess.feature.game.session.GameSession;
import chess.feature.matchmaking.dto.MatchRequest;
import chess.feature.matchmaking.dto.MatchResult;
import chess.feature.matchmaking.enums.MatchStatus;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final GameService gameService;

    private final Object matchmakingLock = new Object();

    private final Map<TimeControl, Deque<String>> waitingQueues =
            new HashMap<>();

    private final Map<String, TimeControl> waitingPlayers =
            new HashMap<>();

    public MatchResult registerPlayer(String userId, MatchRequest request) {
        MatchmakingTimeControlPreset preset = request.preset();

        TimeControl timeControl = new TimeControl(
                preset.getBaseTimeSeconds(),
                preset.getIncrementSeconds()
        );

        synchronized (matchmakingLock) {

            // Không cho người đang chơi đăng ký tìm trận.
            if (gameService.hasActiveGame(userId)) {
                throw new AppException(
                        ErrorCode.ALREADY_IN_GAME,
                        "User is already in an active game."
                );
            }

            // Giữ registration cũ nếu user đã ở trong queue.
            if (waitingPlayers.containsKey(userId)) {
                throw new AppException(
                        ErrorCode.ALREADY_IN_QUEUE,
                        "User is already in a queue."
                );
            }

            Deque<String> queue = waitingQueues.computeIfAbsent(
                    timeControl,
                    key -> new ArrayDeque<>()
            );

            String opponentId = pollNextWaitingPlayer(queue, timeControl);

            // Không tìm thấy đối thủ.
            if (opponentId == null) {
                queue.addLast(userId);
                waitingPlayers.put(userId, timeControl);

                return new MatchResult(false, null, null);
            }

            try {
                CreateGameRequest createGameRequest = new CreateGameRequest(
                        preset.getBaseTimeSeconds(),
                        preset.getIncrementSeconds(),
                        opponentId,
                        userId
                );

                GameSession session = gameService.createGame(createGameRequest);

                // Xóa đối thủ khỏi waiting list sau khi tạo game thành công.
                waitingPlayers.remove(opponentId);

                return new MatchResult(true, opponentId, session);

            } catch (RuntimeException exception) {
                // Khôi phục đối thủ nếu tạo game thất bại.
                queue.addFirst(opponentId);
                throw exception;
            }
        }
    }

    public boolean removePlayer(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        synchronized (matchmakingLock) {

            TimeControl timeControl = waitingPlayers.remove(userId);

            if (timeControl == null) {
                return false;
            }

            Deque<String> queue = waitingQueues.get(timeControl);

            if (queue != null) {
                queue.removeFirstOccurrence(userId);
            }

            return true;
        }
    }

    public MatchStatus getPlayerStatus(String userId) {
        synchronized (matchmakingLock) {
            if (waitingPlayers.containsKey(userId)) {
                return MatchStatus.SEARCHING;
            }

            if (gameService.hasActiveGame(userId)) {
                return MatchStatus.MATCHED;
            }

            return MatchStatus.NOT_SEARCHING;
        }
    }

    private String pollNextWaitingPlayer(
            Deque<String> queue,
            TimeControl timeControl
    ) {
        String candidate;

        while ((candidate = queue.pollFirst()) != null) {

            if (!timeControl.equals(waitingPlayers.get(candidate))) {
                continue;
            }

            return candidate;
        }

        return null;
    }
}