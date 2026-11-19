package chess.feature.gamehistory.controller;

import chess.common.dto.ApiResponse;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game-histories")
public class GameHistoryController {

    private final GameHistoryService gameHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<GameHistoryDto>>> getGameHistory(
            Principal principal,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<GameHistoryDto> result = gameHistoryService.getGameHistory(
                principal.getName(),
                page
        );

        return ResponseEntity.ok(ApiResponse.ok(new PagedModel<>(result)));
    }
}