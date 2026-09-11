package chess.controller;

import chess.dto.request.MoveRequest;
import chess.dto.response.ApiResponse;
import chess.dto.response.GameStateResponse;
import chess.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;

    // 1. Tạo ván đấu mới
    @PostMapping
    public ResponseEntity<ApiResponse<GameStateResponse>> createGame() {
        GameStateResponse response = gameService.createGame();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Game created successfully", response));
    }

    // 2. Lấy thông tin ván đấu
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getGame(@PathVariable String id) {
        GameStateResponse response = gameService.getGame(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 3. Thực hiện nước đi
    @PostMapping("/{id}/moves")
    public ResponseEntity<ApiResponse<GameStateResponse>> makeMove(
            @PathVariable String id,
            @Valid @RequestBody MoveRequest request
    ) {
        GameStateResponse response = gameService.makeMove(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Move executed", response));
    }

    // 4. Lấy danh sách ô hợp lệ để gợi ý khi click quân cờ
    @GetMapping("/{id}/legal-moves")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getLegalMoves(
            @PathVariable String id
    ) {
        Map<String, List<String>> response = gameService.getLegalMoves(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 5. Hoàn tác nước đi
    @PostMapping("/{id}/undo")
    public ResponseEntity<ApiResponse<GameStateResponse>> undoMove(@PathVariable String id) {
        GameStateResponse response = gameService.undoMove(id);
        return ResponseEntity.ok(ApiResponse.ok("Undo successful", response));
    }
}