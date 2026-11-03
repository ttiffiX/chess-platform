package chess.security;

import chess.common.redis.RefreshTokenService;
import chess.common.utils.CookieUtils;
import chess.feature.user.entity.User;
import chess.feature.user.enums.Role;
import chess.feature.user.service.UserService;
import chess.security.jwt.JwtService;
import chess.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CookieUtils cookieUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2AuthenticationSuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String REDIRECT_URI =
            "http://localhost:5173/oauth2/callback";

    private static final String GOOGLE_ID = "google-123";
    private static final String EMAIL = "test@gmail.com";
    private static final String NAME = "Test User";
    private static final String PICTURE = "avatar-url";

    private static final String USER_ID = "user-123";
    private static final String REFRESH_TOKEN = "new-refresh-token";

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private User user;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                userService,
                jwtService,
                refreshTokenService,
                cookieUtils
        );

        ReflectionTestUtils.setField(
                handler,
                "redirectUri",
                REDIRECT_URI
        );

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        user = User.builder()
                .id(USER_ID)
                .googleId(GOOGLE_ID)
                .email(EMAIL)
                .name(NAME)
                .avatarUrl(PICTURE)
                .role(Role.ROLE_USER)
                .banned(false)
                .build();
    }

    /*
     * KỊCH BẢN TEST:
     *
     * 1. Invalid principal:
     *    - Principal không phải OAuth2User.
     *    - Redirect với error=invalid_oauth_user.
     *
     * 2. Invalid Google attributes:
     *    - Thiếu Google ID.
     *    - Thiếu email.
     *    - Redirect với error=invalid_oauth_attributes.
     *
     * 3. Banned user:
     *    - UserService trả về user bị ban.
     *    - Redirect với error=banned.
     *    - Không sinh hoặc lưu refresh token.
     *
     * 4. Successful OAuth2 login:
     *    - Lưu/cập nhật user.
     *    - Sinh refresh token.
     *    - Lưu refresh token vào Redis với TTL.
     *    - Tạo HttpOnly cookie.
     *    - Set-Cookie vào response.
     *    - Redirect về frontend.
     */

    @Nested
    @DisplayName("Invalid OAuth2 authentication tests")
    class InvalidAuthenticationTests {

        @Test
        @DisplayName("Redirect when principal is not OAuth2User")
        void onAuthenticationSuccess_InvalidPrincipal_RedirectsWithError()
                throws Exception {

            when(authentication.getPrincipal())
                    .thenReturn("invalid-principal");

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            assertEquals(
                    REDIRECT_URI + "?error=invalid_oauth_user",
                    response.getRedirectedUrl()
            );

            verifyNoInteractions(
                    userService,
                    jwtService,
                    refreshTokenService,
                    cookieUtils
            );
        }

        @Test
        @DisplayName("Redirect when Google ID is missing")
        void onAuthenticationSuccess_MissingGoogleId_RedirectsWithError()
                throws Exception {

            mockOAuth2User(null, EMAIL);

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            assertEquals(
                    REDIRECT_URI + "?error=invalid_oauth_attributes",
                    response.getRedirectedUrl()
            );

            verifyNoInteractions(
                    userService,
                    jwtService,
                    refreshTokenService,
                    cookieUtils
            );
        }

        @Test
        @DisplayName("Redirect when email is missing")
        void onAuthenticationSuccess_MissingEmail_RedirectsWithError()
                throws Exception {

            mockOAuth2User(GOOGLE_ID, null);

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            assertEquals(
                    REDIRECT_URI + "?error=invalid_oauth_attributes",
                    response.getRedirectedUrl()
            );

            verifyNoInteractions(
                    userService,
                    jwtService,
                    refreshTokenService,
                    cookieUtils
            );
        }
    }

    @Nested
    @DisplayName("Banned user tests")
    class BannedUserTests {

        @Test
        @DisplayName("Redirect when OAuth2 user is banned")
        void onAuthenticationSuccess_BannedUser_RedirectsWithError()
                throws Exception {

            user.setBanned(true);

            mockOAuth2User(GOOGLE_ID, EMAIL);

            when(userService.processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE))
                    .thenReturn(user);

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            assertEquals(
                    REDIRECT_URI + "?error=banned",
                    response.getRedirectedUrl()
            );

            verify(userService).processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE);

            verifyNoInteractions(
                    jwtService,
                    refreshTokenService,
                    cookieUtils
            );
        }
    }

    @Nested
    @DisplayName("Successful OAuth2 authentication tests")
    class SuccessfulAuthenticationTests {

        @Test
        @DisplayName("Generate and save refresh token, set cookie and redirect")
        void onAuthenticationSuccess_Success() throws Exception {
            mockOAuth2User(GOOGLE_ID, EMAIL);

            when(userService.processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE))
                    .thenReturn(user);

            when(jwtService.generateRefreshToken(USER_ID))
                    .thenReturn(REFRESH_TOKEN);

            when(jwtService.getRefreshTokenTtl())
                    .thenReturn(REFRESH_TTL);

            ResponseCookie cookie = ResponseCookie.from(
                            "refreshToken", REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/api/auth/refresh")
                    .maxAge(REFRESH_TTL)
                    .build();

            when(cookieUtils.createRefreshTokenCookie(
                    REFRESH_TOKEN, REFRESH_TTL))
                    .thenReturn(cookie);

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            assertEquals(REDIRECT_URI, response.getRedirectedUrl());

            assertEquals(
                    cookie.toString(),
                    response.getHeader(HttpHeaders.SET_COOKIE)
            );

            verify(userService).processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE);

            verify(jwtService).generateRefreshToken(USER_ID);

            verify(refreshTokenService).save(
                    USER_ID, REFRESH_TOKEN, REFRESH_TTL);

            verify(cookieUtils).createRefreshTokenCookie(
                    REFRESH_TOKEN, REFRESH_TTL);
        }

        @Test
        @DisplayName("Save refresh token with correct TTL")
        void onAuthenticationSuccess_SavesCorrectTtl() throws Exception {
            mockOAuth2User(GOOGLE_ID, EMAIL);

            when(userService.processOAuthPostLogin(
                    GOOGLE_ID, EMAIL, NAME, PICTURE))
                    .thenReturn(user);

            when(jwtService.generateRefreshToken(USER_ID))
                    .thenReturn(REFRESH_TOKEN);

            when(jwtService.getRefreshTokenTtl())
                    .thenReturn(REFRESH_TTL);

            when(cookieUtils.createRefreshTokenCookie(anyString(), any()))
                    .thenReturn(ResponseCookie.from(
                            "refreshToken", REFRESH_TOKEN).build());

            handler.onAuthenticationSuccess(
                    request,
                    response,
                    authentication
            );

            ArgumentCaptor<Duration> ttlCaptor =
                    ArgumentCaptor.forClass(Duration.class);

            verify(refreshTokenService).save(
                    eq(USER_ID),
                    eq(REFRESH_TOKEN),
                    ttlCaptor.capture()
            );

            assertEquals(REFRESH_TTL, ttlCaptor.getValue());
        }
    }

    private void mockOAuth2User(String googleId, String email) {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(oAuth2User.getAttribute("sub")).thenReturn(googleId);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        if (googleId != null && email != null) {
            when(oAuth2User.getAttribute("name")).thenReturn(NAME);
            when(oAuth2User.getAttribute("picture")).thenReturn(PICTURE);
        }
    }
}