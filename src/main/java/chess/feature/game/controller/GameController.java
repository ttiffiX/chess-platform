package chess.feature.game.controller;

import chess.common.dto.ApiResponse;
import chess.feature.game.dto.response.GameStateResponse;
import chess.feature.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    /**
     * Lấy trạng thái bàn cờ khi user mới vào phòng hoặc F5.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getGame(
            @PathVariable String id,
            Principal principal
    ) {
        GameStateResponse response = gameService.getGame(id, principal.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Xem snapshot lịch sử thế cờ cũ (Read-only).
     */
    @GetMapping("/{id}/history/{moveIndex}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getSnapshotAt(
            @PathVariable String id,
            @PathVariable int moveIndex,
            Principal principal
    ) {
        GameStateResponse response = gameService.getGameSnapshotAt(id, principal.getName(), moveIndex);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}