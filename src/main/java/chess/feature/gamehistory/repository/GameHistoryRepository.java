package chess.feature.gamehistory.repository;

import chess.feature.gamehistory.entity.GameHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameHistoryRepository extends MongoRepository<GameHistory, String> {

    Page<GameHistory> findByPlayerIdsContaining(
            String playerId,
            Pageable pageable
    );
}
