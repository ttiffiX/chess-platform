package chess.feature.game.controller;

import chess.common.dto.ApiResponse;
import chess.feature.game.dto.response.GameResponse;
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
    public ResponseEntity<ApiResponse<GameResponse>> getGame(
            @PathVariable String id,
            Principal principal
    ) {
        GameResponse response = gameService.getGame(id, principal.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<String>> getActiveGame(
            Principal principal
    ) {
        String activeGameId = gameService.getActiveGameId(principal.getName());

        return ResponseEntity.ok(ApiResponse.ok(activeGameId));
    }
}