package chess.feature.gamehistory.service;

import chess.feature.gamehistory.entity.GameHistory;
import chess.feature.gamehistory.repository.GameHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameHistoryService {
    private final GameHistoryRepository gameHistoryRepository;

    public void saveGameHistory(GameHistory gameHistory) {
        gameHistoryRepository.save(gameHistory);
    }

    public GameHistory getGameHistory(String gameId) {
        // Lấy lịch sử trò chơi từ cơ sở dữ liệu hoặc nơi lưu trữ khác.
        return null;
    }
}
