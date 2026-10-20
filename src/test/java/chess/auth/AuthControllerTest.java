package chess.auth;

import chess.exception.AppException;
import chess.exception.ErrorCode;
import chess.exception.GlobalExceptionHandler;
import chess.feature.auth.controller.AuthController;
import chess.feature.auth.dto.AuthResponse;
import chess.feature.auth.dto.TokenRefreshResult;
import chess.feature.auth.service.AuthService;
import chess.security.jwt.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String BASE_URL = "/api/auth";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private TokenRefreshResult createSampleRefreshResult() {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .tokenType("Bearer")
                .userInfo(AuthResponse.UserInfo.builder()
                        .id("user-123")
                        .email("test@gmail.com")
                        .name("Test User")
                        .role("USER")
                        .build())
                .build();

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "new-refresh-token")
                .httpOnly(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        return TokenRefreshResult.builder()
                .authResponse(authResponse)
                .refreshTokenCookie(cookie)
                .build();
    }

    @Nested
    @DisplayName("Tests for Successful Flows (2xx)")
    class SuccessTests {

        @Test
        @DisplayName("POST /api/auth/refresh - Refresh token thành công trả về 200 kèm Set-Cookie và dữ liệu token mới")
        void refreshToken_Success() throws Exception {
            when(authService.refreshToken("valid-token")).thenReturn(createSampleRefreshResult());

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .cookie(new Cookie("refreshToken", "valid-token")))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.data.userInfo.email").value("test@gmail.com"));
        }

        @Test
        @DisplayName("POST /api/auth/logout - Đăng xuất thành công trả về 200 kèm cookie xóa session")
        void logout_Success() throws Exception {
            ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
                    .maxAge(0)
                    .build();
            when(authService.logout(any())).thenReturn(cleanCookie);

            mockMvc.perform(post(BASE_URL + "/logout")
                            .cookie(new Cookie("refreshToken", "valid-token")))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    @Nested
    @DisplayName("Tests for Exception Handling (4xx)")
    class ExceptionTests {

        @Test
        @DisplayName("POST /api/auth/refresh - Thất bại khi thiếu Cookie -> Trả về 401 kèm mã lỗi 4002 (REFRESH_TOKEN_MISSING)")
        void missingCookie_ReturnsUnauthorized() throws Exception {
            // Giả lập service ném lỗi thiếu token khi không có cookie truyền xuống
            when(authService.refreshToken(null))
                    .thenThrow(new AppException(ErrorCode.REFRESH_TOKEN_MISSING, "Missing refresh token cookie"));

            mockMvc.perform(post(BASE_URL + "/refresh")) // Không gửi cookie
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4002));
        }

        @Test
        @DisplayName("POST /api/auth/refresh - Thất bại khi Token không hợp lệ -> Trả về 401 kèm mã lỗi 4003 (REFRESH_TOKEN_INVALID)")
        void invalidToken_ReturnsUnauthorized() throws Exception {
            when(authService.refreshToken("invalid-token"))
                    .thenThrow(new AppException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token không hợp lệ"));

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .cookie(new Cookie("refreshToken", "invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value(4003));
        }
    }
}