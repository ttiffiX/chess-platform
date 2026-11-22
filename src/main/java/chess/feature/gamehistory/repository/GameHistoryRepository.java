package chess.feature.gamehistory.repository;

import chess.feature.gamehistory.entity.GameHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameHistoryRepository extends MongoRepository<GameHistory, String> {

    Page<GameHistory> findByPlayerIdsContaining(
            String playerId,
            Pageable pageable
    );

    Optional<GameHistory> findByGameIdAndPlayerIdsContaining(String gameId, String playerId);
}
