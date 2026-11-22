package chess.feature.analysis.controller;

import chess.common.dto.ApiResponse;
import chess.feature.analysis.service.GameAnalysisService;
import chess.feature.analysis.dto.GameReplayDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game-analysis")
public class GameAnalysisController {
    private final GameAnalysisService gameAnalysisService;

    @GetMapping("/{gameId}/replay")
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
}
