package chess.security;

import chess.security.config.JwtAuthenticationFilter;
import chess.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String REFRESH_TOKEN = "valid-refresh-token";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /*
     * KỊCH BẢN TEST:
     *
     * 1. Access token hợp lệ:
     *    - Filter lấy token từ Authorization Bearer.
     *    - Gọi validateAccessToken().
     *    - Tạo Authentication với userId và role.
     *
     * 2. Refresh token gửi qua Bearer:
     *    - validateAccessToken() trả false.
     *    - Không tạo Authentication.
     *
     * 3. Token không hợp lệ:
     *    - Không tạo Authentication.
     *
     * 4. Không có Authorization header:
     *    - Không gọi JwtService.
     *    - Không tạo Authentication.
     *
     * 5. Filter chain luôn được tiếp tục.
     */

    @Nested
    @DisplayName("Valid access token tests")
    class ValidAccessTokenTests {

        @Test
        @DisplayName("Authenticate request with valid access token")
        void doFilter_ValidAccessToken_SetsAuthentication() throws Exception {
            request.addHeader(
                    "Authorization",
                    "Bearer " + ACCESS_TOKEN
            );

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("user-123");

            when(jwtService.getRoleFromToken(ACCESS_TOKEN))
                    .thenReturn("ROLE_USER");

            filter.doFilter(request, response, filterChain);

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            assertNotNull(authentication);
            assertEquals("user-123", authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_USER")));

            verify(jwtService).validateAccessToken(ACCESS_TOKEN);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Use default ROLE_USER when token has no role")
        void doFilter_NullRole_UsesDefaultRole() throws Exception {
            request.addHeader(
                    "Authorization",
                    "Bearer " + ACCESS_TOKEN
            );

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("user-123");

            when(jwtService.getRoleFromToken(ACCESS_TOKEN))
                    .thenReturn(null);

            filter.doFilter(request, response, filterChain);

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            assertNotNull(authentication);
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_USER")));
        }
    }

    @Nested
    @DisplayName("Invalid token tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Reject refresh token sent through Bearer header")
        void doFilter_RefreshToken_DoesNotAuthenticate() throws Exception {
            request.addHeader(
                    "Authorization",
                    "Bearer " + REFRESH_TOKEN
            );

            when(jwtService.validateAccessToken(REFRESH_TOKEN))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verify(jwtService).validateAccessToken(REFRESH_TOKEN);
            verify(jwtService, never()).getUserIdFromToken(anyString());
            verify(jwtService, never()).getRoleFromToken(anyString());

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Do not authenticate request with invalid token")
        void doFilter_InvalidToken_DoesNotAuthenticate() throws Exception {
            request.addHeader(
                    "Authorization",
                    "Bearer invalid-token"
            );

            when(jwtService.validateAccessToken("invalid-token"))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verify(jwtService, never()).getUserIdFromToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Missing authorization header tests")
    class MissingHeaderTests {

        @Test
        @DisplayName("Skip JWT authentication when Authorization header is missing")
        void doFilter_MissingHeader_SkipsAuthentication() throws Exception {
            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verifyNoInteractions(jwtService);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Skip JWT authentication when header is not Bearer")
        void doFilter_NonBearerHeader_SkipsAuthentication() throws Exception {
            request.addHeader("Authorization", "Basic abc123");

            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verifyNoInteractions(jwtService);
            verify(filterChain).doFilter(request, response);
        }
    }
}