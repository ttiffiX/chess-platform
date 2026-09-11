package chess.controller;

import chess.dto.request.MoveRequest;
import chess.dto.response.GameStateResponse;
import chess.engine.model.game.GameResult;
import chess.engine.model.piece.Color;
import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final String BASE_URL = "/api/games";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    private GameStateResponse createSampleGameState(String gameId, Color turn) {
        return new GameStateResponse(
                gameId,
                GameResult.inProgress(),
                turn,
                false,
                1,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of("e2", List.of("e3", "e4")),
                Instant.now()
        );
    }

    @Nested
    @DisplayName("Tests for Successful Flows (2xx)")
    class SuccessTests {

        @Test
        @DisplayName("POST /api/games - Tạo game thành công trả về 201")
        void createGame_Success() throws Exception {
            GameStateResponse dummyResponse = createSampleGameState("game-123", Color.WHITE);
            when(gameService.createGame()).thenReturn(dummyResponse);

            mockMvc.perform(post(BASE_URL))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.gameId").value("game-123"))
                    .andExpect(jsonPath("$.data.currentTurn").value("WHITE"))
                    .andExpect(jsonPath("$.data.legalMoves.e2[0]").value("e3"));
        }

        @Test
        @DisplayName("GET /api/games/{id} - Lấy thông tin ván đấu trả về 200")
        void getGame_Success() throws Exception {
            GameStateResponse dummyResponse = createSampleGameState("game-123", Color.WHITE);
            when(gameService.getGame("game-123")).thenReturn(dummyResponse);

            mockMvc.perform(get(BASE_URL + "/game-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.gameId").value("game-123"));
        }

        @Test
        @DisplayName("POST /api/games/{id}/moves - Đi cờ thành công trả về 200")
        void makeMove_Success() throws Exception {
            MoveRequest request = new MoveRequest("e2", "e4", null);
            GameStateResponse dummyResponse = createSampleGameState("game-123", Color.BLACK);

            when(gameService.makeMove(eq("game-123"), any(MoveRequest.class))).thenReturn(dummyResponse);

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentTurn").value("BLACK"));
        }

        @Test
        @DisplayName("GET /api/games/{id}/legal-moves - Lấy danh sách nước đi gợi ý trả về 200")
        void getLegalMoves_Success() throws Exception {
            Map<String, List<String>> legalMoves = Map.of("e2", List.of("e3", "e4"));
            when(gameService.getLegalMoves("game-123")).thenReturn(legalMoves);

            mockMvc.perform(get(BASE_URL + "/game-123/legal-moves"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.e2[0]").value("e3"));
        }

        @Test
        @DisplayName("POST /api/games/{id}/undo - Hoàn tác nước đi trả về 200")
        void undoMove_Success() throws Exception {
            GameStateResponse dummyResponse = createSampleGameState("game-123", Color.WHITE);
            when(gameService.undoMove("game-123")).thenReturn(dummyResponse);

            mockMvc.perform(post(BASE_URL + "/game-123/undo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tests for Exception Handling & Validation (4xx)")
    class ExceptionTests {

        @Test
        @DisplayName("Game không tồn tại -> Trả về 404 và ErrorCode 2001")
        void gameNotFound_Returns404() throws Exception {
            when(gameService.getGame("unknown-id"))
                    .thenThrow(new AppException(ErrorCode.ROOM_NOT_FOUND, "Không tìm thấy phòng chơi"));

            mockMvc.perform(get(BASE_URL + "/unknown-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(2001));
        }

        @Test
        @DisplayName("Nước đi sai luật -> Trả về 400 và ErrorCode 1001")
        void illegalMove_Returns400() throws Exception {
            MoveRequest request = new MoveRequest("e2", "e5", null);
            when(gameService.makeMove(eq("game-123"), any(MoveRequest.class)))
                    .thenThrow(new AppException(ErrorCode.INVALID_MOVE, "Nước đi không hợp lệ"));

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(1001));
        }

        @Test
        @DisplayName("DTO sai format tọa độ (@Pattern) -> Chặn tại 400, ErrorCode 4000")
        void invalidPositionPattern_Returns400() throws Exception {
            String invalidPayload = """
                    {
                        "from": "z9",
                        "to": "e4"
                    }
                    """;

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4000));
        }

        @Test
        @DisplayName("Gửi sai HTTP method -> Trả về 405 và ErrorCode 4005")
        void unsupportedMethod_Returns405() throws Exception {
            mockMvc.perform(delete(BASE_URL))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.errorCode").value(4005));
        }

        @Test
        @DisplayName("DTO thiếu trường bắt buộc -> Chặn 400 Bad Request, ErrorCode 4000")
        void missingRequiredFields_Returns400() throws Exception {
            String payloadMissingFrom = """
            {
                "to": "e4"
            }
            """;

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payloadMissingFrom))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4000));
        }

        @Test
        @DisplayName("Enum promotionType sai giá trị -> Trả về 400 và ErrorCode 4000")
        void invalidPromotionEnum_Returns400() throws Exception {
            String invalidEnumPayload = """
            {
                "from": "e7",
                "to": "e8",
                "promotionType": "SUPER_QUEEN"
            }
            """;

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidEnumPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4000));
        }

        @Test
        @DisplayName("Sai hoàn toàn URL -> Bắt NoResourceFoundException, trả về 404")
        void wrongUrlPath_Returns404() throws Exception {
            mockMvc.perform(get("/api/games/not-exist/invalid-sub-path"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4004));
        }

        @Test
        @DisplayName("Lỗi hệ thống không mong đợi -> Trả về 500 Internal Server Error và ErrorCode 5000")
        void unexpectedError_Returns500() throws Exception {
            when(gameService.createGame()).thenThrow(new RuntimeException("Database connection timeout"));

            mockMvc.perform(post(BASE_URL))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(5000));
        }

        @Test
        @DisplayName("Tọa độ chứa chuỗi rỗng/khoảng trắng -> Báo lỗi 400 Bad Request")
        void blankCoordinates_Returns400() throws Exception {
            String blankPayload = """
            {
                "from": "   ",
                "to": ""
            }
            """;

            mockMvc.perform(post(BASE_URL + "/game-123/moves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(blankPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(4000));
        }
    }
}