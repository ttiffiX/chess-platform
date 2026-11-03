package chess.auth;

import chess.common.redis.RefreshTokenService;
import chess.common.utils.CookieUtils;
import chess.exception.AppException;
import chess.feature.auth.dto.TokenRefreshResult;
import chess.feature.auth.service.AuthService;
import chess.feature.user.entity.User;
import chess.feature.user.enums.Role;
import chess.feature.user.service.UserService;
import chess.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private CookieUtils cookieUtils;

    @InjectMocks
    private AuthService authService;

    private User user;

    private static final String USER_ID = "user-123";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("test@gmail.com")
                .name("Test User")
                .avatarUrl("avatar-url")
                .role(Role.ROLE_USER)
                .banned(false)
                .build();
    }

    /*
     * KỊCH BẢN TEST:
     *
     * A. REFRESH TOKEN FAILURE:
     *    1. Token null hoặc blank -> REFRESH_TOKEN_MISSING.
     *    2. JWT không hợp lệ -> REFRESH_TOKEN_INVALID.
     *    3. Token không khớp Redis -> REFRESH_TOKEN_INVALID.
     *    4. User không tồn tại -> UNAUTHORIZED.
     *    5. User bị ban -> xóa refresh token trong Redis và UNAUTHORIZED.
     *
     * B. REFRESH TOKEN SUCCESS:
     *    1. Validate refresh token.
     *    2. Lấy userId từ JWT.
     *    3. Đối chiếu token trong Redis.
     *    4. Lấy User.
     *    5. Sinh access token mới và refresh token mới.
     *    6. Lưu refresh token mới vào Redis với TTL.
     *    7. Tạo cookie mới.
     *    8. Trả về AuthResponse chứa access token và user info.
     *
     * C. LOGOUT:
     *    1. Token hợp lệ -> xóa token Redis.
     *    2. Token không hợp lệ -> không xóa Redis.
     *    3. Token null/blank -> vẫn trả cookie xóa.
     */

    @Nested
    @DisplayName("Refresh token failure tests")
    class RefreshFailureTests {

        @Test
        @DisplayName("Throw exception when refresh token is null")
        void refreshToken_NullToken_ThrowsException() {
            assertThrows(AppException.class,
                    () -> authService.refreshToken(null));

            verifyNoInteractions(jwtService, refreshTokenService, userService);
        }

        @Test
        @DisplayName("Throw exception when refresh token is blank")
        void refreshToken_BlankToken_ThrowsException() {
            assertThrows(AppException.class,
                    () -> authService.refreshToken("   "));

            verifyNoInteractions(jwtService, refreshTokenService, userService);
        }

        @Test
        @DisplayName("Reject invalid JWT refresh token")
        void refreshToken_InvalidJwt_ThrowsException() {
            when(jwtService.validateRefreshToken(OLD_REFRESH_TOKEN))
                    .thenReturn(false);

            assertThrows(AppException.class,
                    () -> authService.refreshToken(OLD_REFRESH_TOKEN));

            verify(jwtService).validateRefreshToken(OLD_REFRESH_TOKEN);
            verifyNoInteractions(refreshTokenService, userService);
        }

        @Test
        @DisplayName("Reject refresh token not matching Redis")
        void refreshToken_TokenNotMatchingRedis_ThrowsException() {
            mockValidRefreshToken();

            when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN))
                    .thenReturn(false);

            assertThrows(AppException.class,
                    () -> authService.refreshToken(OLD_REFRESH_TOKEN));

            verify(refreshTokenService).matches(USER_ID, OLD_REFRESH_TOKEN);
            verifyNoInteractions(userService);

            verify(refreshTokenService, never()).save(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Reject refresh token when user does not exist")
        void refreshToken_UserNotFound_ThrowsException() {
            mockValidRefreshToken();

            when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN))
                    .thenReturn(true);

            when(userService.findById(USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class,
                    () -> authService.refreshToken(OLD_REFRESH_TOKEN));

            verify(userService).findById(USER_ID);
            verify(refreshTokenService, never()).save(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Delete refresh token and reject banned user")
        void refreshToken_UserBanned_DeletesTokenAndThrowsException() {
            user.setBanned(true);

            mockValidRefreshToken();

            when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN))
                    .thenReturn(true);

            when(userService.findById(USER_ID))
                    .thenReturn(Optional.of(user));

            assertThrows(AppException.class,
                    () -> authService.refreshToken(OLD_REFRESH_TOKEN));

            verify(refreshTokenService).delete(USER_ID);

            verify(jwtService, never())
                    .generateAccessToken(anyString(), anyString(), anyString());

            verify(jwtService, never()).generateRefreshToken(anyString());
            verify(refreshTokenService, never()).save(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Refresh token success tests")
    class RefreshSuccessTests {

        @Test
        @DisplayName("Refresh successfully and rotate refresh token")
        void refreshToken_Success() {
            mockValidRefreshToken();

            when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN))
                    .thenReturn(true);

            when(userService.findById(USER_ID))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(
                    USER_ID, user.getEmail(), Role.ROLE_USER.name()))
                    .thenReturn(NEW_ACCESS_TOKEN);

            when(jwtService.generateRefreshToken(USER_ID))
                    .thenReturn(NEW_REFRESH_TOKEN);

            when(jwtService.getRefreshTokenTtl())
                    .thenReturn(REFRESH_TTL);

            ResponseCookie cookie = ResponseCookie.from(
                            "refreshToken", NEW_REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/api/auth/refresh")
                    .maxAge(REFRESH_TTL)
                    .build();

            when(cookieUtils.createRefreshTokenCookie(
                    NEW_REFRESH_TOKEN, REFRESH_TTL))
                    .thenReturn(cookie);

            TokenRefreshResult result =
                    authService.refreshToken(OLD_REFRESH_TOKEN);

            assertNotNull(result);
            assertNotNull(result.getAuthResponse());
            assertEquals(NEW_ACCESS_TOKEN,
                    result.getAuthResponse().getAccessToken());
            assertEquals("Bearer",
                    result.getAuthResponse().getTokenType());

            assertEquals(USER_ID,
                    result.getAuthResponse().getUserInfo().getId());
            assertEquals("test@gmail.com",
                    result.getAuthResponse().getUserInfo().getEmail());
            assertEquals("Test User",
                    result.getAuthResponse().getUserInfo().getName());
            assertEquals("ROLE_USER",
                    result.getAuthResponse().getUserInfo().getRole());

            assertNotNull(result.getRefreshTokenCookie());
            assertEquals(cookie, result.getRefreshTokenCookie());

            verify(jwtService).validateRefreshToken(OLD_REFRESH_TOKEN);
            verify(jwtService).getUserIdFromToken(OLD_REFRESH_TOKEN);
            verify(refreshTokenService).matches(USER_ID, OLD_REFRESH_TOKEN);

            verify(jwtService).generateAccessToken(
                    USER_ID, "test@gmail.com", "ROLE_USER");

            verify(jwtService).generateRefreshToken(USER_ID);

            verify(refreshTokenService).save(
                    USER_ID, NEW_REFRESH_TOKEN, REFRESH_TTL);

            verify(cookieUtils).createRefreshTokenCookie(
                    NEW_REFRESH_TOKEN, REFRESH_TTL);
        }

        @Test
        @DisplayName("Save rotated refresh token with configured TTL")
        void refreshToken_Success_SavesCorrectTtl() {
            mockValidRefreshToken();

            when(refreshTokenService.matches(USER_ID, OLD_REFRESH_TOKEN))
                    .thenReturn(true);

            when(userService.findById(USER_ID))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn(NEW_ACCESS_TOKEN);

            when(jwtService.generateRefreshToken(USER_ID))
                    .thenReturn(NEW_REFRESH_TOKEN);

            when(jwtService.getRefreshTokenTtl())
                    .thenReturn(REFRESH_TTL);

            when(cookieUtils.createRefreshTokenCookie(anyString(), any()))
                    .thenReturn(ResponseCookie.from("refreshToken", NEW_REFRESH_TOKEN).build());

            authService.refreshToken(OLD_REFRESH_TOKEN);

            ArgumentCaptor<Duration> ttlCaptor =
                    ArgumentCaptor.forClass(Duration.class);

            verify(refreshTokenService).save(
                    eq(USER_ID),
                    eq(NEW_REFRESH_TOKEN),
                    ttlCaptor.capture()
            );

            assertEquals(REFRESH_TTL, ttlCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("Logout tests")
    class LogoutTests {

        @Test
        @DisplayName("Delete Redis refresh token when JWT is valid")
        void logout_ValidToken_DeletesRedisToken() {
            ResponseCookie cleanCookie =
                    ResponseCookie.from("refreshToken", "")
                            .maxAge(0)
                            .build();

            when(jwtService.validateRefreshToken(OLD_REFRESH_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(OLD_REFRESH_TOKEN))
                    .thenReturn(USER_ID);

            when(cookieUtils.deleteRefreshTokenCookie())
                    .thenReturn(cleanCookie);

            ResponseCookie result = authService.logout(OLD_REFRESH_TOKEN);

            assertEquals(cleanCookie, result);

            verify(refreshTokenService).delete(USER_ID);
            verify(cookieUtils).deleteRefreshTokenCookie();
        }

        @Test
        @DisplayName("Do not delete Redis token when JWT is invalid")
        void logout_InvalidToken_DoesNotDeleteRedisToken() {
            when(jwtService.validateRefreshToken(OLD_REFRESH_TOKEN))
                    .thenReturn(false);

            ResponseCookie cleanCookie =
                    ResponseCookie.from("refreshToken", "")
                            .maxAge(0)
                            .build();

            when(cookieUtils.deleteRefreshTokenCookie())
                    .thenReturn(cleanCookie);

            ResponseCookie result = authService.logout(OLD_REFRESH_TOKEN);

            assertEquals(cleanCookie, result);

            verify(refreshTokenService, never()).delete(anyString());
            verify(jwtService, never()).getUserIdFromToken(anyString());
            verify(cookieUtils).deleteRefreshTokenCookie();
        }

        @Test
        @DisplayName("Delete cookie when refresh token is null")
        void logout_NullToken_DeletesCookieOnly() {
            ResponseCookie cleanCookie =
                    ResponseCookie.from("refreshToken", "")
                            .maxAge(0)
                            .build();

            when(cookieUtils.deleteRefreshTokenCookie())
                    .thenReturn(cleanCookie);

            ResponseCookie result = authService.logout(null);

            assertEquals(cleanCookie, result);

            verifyNoInteractions(jwtService, refreshTokenService);
            verify(cookieUtils).deleteRefreshTokenCookie();
        }

        @Test
        @DisplayName("Delete cookie when refresh token is blank")
        void logout_BlankToken_DeletesCookieOnly() {
            ResponseCookie cleanCookie =
                    ResponseCookie.from("refreshToken", "")
                            .maxAge(0)
                            .build();

            when(cookieUtils.deleteRefreshTokenCookie())
                    .thenReturn(cleanCookie);

            ResponseCookie result = authService.logout("   ");

            assertEquals(cleanCookie, result);

            verifyNoInteractions(jwtService, refreshTokenService);
        }
    }

    private void mockValidRefreshToken() {
        when(jwtService.validateRefreshToken(OLD_REFRESH_TOKEN))
                .thenReturn(true);

        when(jwtService.getUserIdFromToken(OLD_REFRESH_TOKEN))
                .thenReturn(USER_ID);
    }
}