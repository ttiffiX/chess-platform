package chess.feature.gamehistory.controller;

import chess.common.dto.ApiResponse;
import chess.feature.gamehistory.dto.GameHistoryDto;
import chess.feature.gamehistory.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game-histories")
public class GameHistoryController {

    private final GameHistoryService gameHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GameHistoryDto>>> getGameHistory(
            Principal principal,
            @PageableDefault(sort = "endedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<GameHistoryDto> result = gameHistoryService.getGameHistory(
                principal.getName(),
                pageable
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}