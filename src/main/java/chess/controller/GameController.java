package chess.controller;

import chess.dto.request.CreateGameRequest;
import chess.dto.request.MoveRequest;
import chess.dto.response.ApiResponse;
import chess.dto.response.GameStateResponse;
import chess.engine.model.piece.Color;
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
    public ResponseEntity<ApiResponse<GameStateResponse>> createGame(
            @Valid @RequestBody(required = false) CreateGameRequest request
    ) {
        GameStateResponse response = gameService.createGame(request);
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
    public ResponseEntity<ApiResponse<GameStateResponse>> undoMove(@PathVariable String id,
                                                                   @RequestParam(required = false) Color color) {
        GameStateResponse response = gameService.undoMove(id, color);
        return ResponseEntity.ok(ApiResponse.ok("Undo successful", response));
    }

    // 6. Xin đầu hàng hoặc đồng ý hòa
    @PostMapping("/{id}/resign")
    public ResponseEntity<ApiResponse<GameStateResponse>> resign(
            @PathVariable String id,
            @RequestParam Color color
    ) {
        GameStateResponse response = gameService.resign(id, color);
        return ResponseEntity.ok(ApiResponse.ok("Resign successful", response));
    }

    // 7. Xin hòa
    @PostMapping("/{id}/draw")
    public ResponseEntity<ApiResponse<GameStateResponse>> draw(@PathVariable String id) {
        GameStateResponse response = gameService.offerDraw(id);
        return ResponseEntity.ok(ApiResponse.ok("Draw agreed", response));
    }

    // 8. Báo hết giờ từ Client
    @PostMapping("/{id}/timeout")
    public ResponseEntity<ApiResponse<GameStateResponse>> claimTimeout(@PathVariable String id) {
        GameStateResponse response = gameService.claimTimeout(id);
        return ResponseEntity.ok(ApiResponse.ok("Timeout processed", response));
    }

    // 9. Xem thế cờ tại một nước đi trong quá khứ (Read-only)
    @GetMapping("/{id}/history/{moveIndex}")
    public ResponseEntity<ApiResponse<GameStateResponse>> getSnapshotAt(
            @PathVariable String id,
            @PathVariable int moveIndex
    ) {
        GameStateResponse response = gameService.getGameSnapshotAt(id, moveIndex);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}