package chess.feature.analysis.controller;

import chess.common.dto.ApiResponse;
import chess.feature.analysis.dto.GameAnalysisDto;
import chess.feature.analysis.dto.GameAnalysisRequest;
import chess.feature.analysis.service.GameAnalysisService;
import chess.feature.analysis.dto.GameReplayDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameAnalysisController {
    private final GameAnalysisService gameAnalysisService;

    @GetMapping("/{gameId}/analysis")
    public ResponseEntity<ApiResponse<GameReplayDto>> getGameReplay(
            @PathVariable String gameId,
            Principal principal
    ) {
        GameReplayDto result = gameAnalysisService.getGameReplay(
                gameId,
                principal.getName()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/analysis/move")
    public ResponseEntity<ApiResponse<GameAnalysisDto>> analyzeMove(
            @Valid @RequestBody GameAnalysisRequest request
    ) {
        GameAnalysisDto result = gameAnalysisService.analyzeMove(request);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
