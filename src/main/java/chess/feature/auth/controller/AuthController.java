package chess.feature.auth.controller;

import chess.common.dto.ApiResponse;
import chess.feature.auth.dto.AuthResponse;
import chess.feature.auth.dto.TokenRefreshResult;
import chess.feature.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        // AuthService tự xử lý kiểm tra và ném AppException nếu token lỗi/hết hạn/bị ban
        TokenRefreshResult result = authService.refreshToken(refreshToken);

        // Ghi đè Cookie mới (Token Rotation)
        response.addHeader(HttpHeaders.SET_COOKIE, result.getRefreshTokenCookie().toString());

        // Trả về ApiResponse chuẩn chứa Access Token + User Info
        return ResponseEntity.ok(ApiResponse.ok(result.getAuthResponse()));
    }
}