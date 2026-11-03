
package chess.security;

import chess.security.config.JwtAuthenticationFilter;
import chess.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.util.Objects;

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

    private void addBearerToken(String token) {
        request.addHeader("Authorization", "Bearer " + token);
    }

    private void assertUnauthorized() throws ServletException, IOException {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, never()).doFilter(request, response);
    }

    // =========================================================
    // VALID ACCESS TOKEN
    // =========================================================

    @Nested
    @DisplayName("Valid access token tests")
    class ValidAccessTokenTests {

        @Test
        @DisplayName("Authenticate request with valid access token")
        void doFilter_ValidAccessToken_SetsAuthentication() throws Exception {
            addBearerToken(ACCESS_TOKEN);

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
                            Objects.equals(authority.getAuthority(), "ROLE_USER")));

            verify(jwtService).validateAccessToken(ACCESS_TOKEN);
            verify(jwtService).getUserIdFromToken(ACCESS_TOKEN);
            verify(jwtService).getRoleFromToken(ACCESS_TOKEN);

            verify(filterChain).doFilter(request, response);

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        }
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Nested
    @DisplayName("Invalid token tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Reject refresh token sent through Bearer header")
        void doFilter_RefreshToken_ReturnsUnauthorized() throws Exception {
            addBearerToken(REFRESH_TOKEN);

            when(jwtService.validateAccessToken(REFRESH_TOKEN))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verify(jwtService).validateAccessToken(REFRESH_TOKEN);
            verify(jwtService, never()).getUserIdFromToken(anyString());
            verify(jwtService, never()).getRoleFromToken(anyString());
        }

        @Test
        @DisplayName("Reject request with invalid access token")
        void doFilter_InvalidToken_ReturnsUnauthorized() throws Exception {
            addBearerToken("invalid-token");

            when(jwtService.validateAccessToken("invalid-token"))
                    .thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verify(jwtService).validateAccessToken("invalid-token");
            verify(jwtService, never()).getUserIdFromToken(anyString());
            verify(jwtService, never()).getRoleFromToken(anyString());
        }

        @Test
        @DisplayName("Reject request when Bearer token is empty")
        void doFilter_EmptyBearerToken_ReturnsUnauthorized() throws Exception {
            request.addHeader("Authorization", "Bearer ");

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verifyNoInteractions(jwtService);
        }
    }

    // =========================================================
    // INVALID USER ID
    // =========================================================

    @Nested
    @DisplayName("Invalid user identity tests")
    class InvalidUserIdentityTests {

        @Test
        @DisplayName("Reject request when userId is null")
        void doFilter_NullUserId_ReturnsUnauthorized() throws Exception {
            addBearerToken(ACCESS_TOKEN);

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verify(jwtService).getUserIdFromToken(ACCESS_TOKEN);
            verify(jwtService, never()).getRoleFromToken(anyString());
        }

        @Test
        @DisplayName("Reject request when userId is blank")
        void doFilter_BlankUserId_ReturnsUnauthorized() throws Exception {
            addBearerToken(ACCESS_TOKEN);

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("   ");

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verify(jwtService).getUserIdFromToken(ACCESS_TOKEN);
            verify(jwtService, never()).getRoleFromToken(anyString());
        }
    }

    // =========================================================
    // INVALID ROLE
    // =========================================================

    @Nested
    @DisplayName("Invalid role tests")
    class InvalidRoleTests {

        @Test
        @DisplayName("Reject request when role is null")
        void doFilter_NullRole_ReturnsUnauthorized() throws Exception {
            addBearerToken(ACCESS_TOKEN);

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("user-123");

            when(jwtService.getRoleFromToken(ACCESS_TOKEN))
                    .thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();
        }

        @Test
        @DisplayName("Reject request when role is blank")
        void doFilter_BlankRole_ReturnsUnauthorized() throws Exception {
            addBearerToken(ACCESS_TOKEN);

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("user-123");

            when(jwtService.getRoleFromToken(ACCESS_TOKEN))
                    .thenReturn("   ");

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();
        }

        @Test
        @DisplayName("Reject request when role does not exist in enum")
        void doFilter_InvalidRole_ReturnsUnauthorized() throws Exception {
            addBearerToken(ACCESS_TOKEN);

            when(jwtService.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(true);

            when(jwtService.getUserIdFromToken(ACCESS_TOKEN))
                    .thenReturn("user-123");

            when(jwtService.getRoleFromToken(ACCESS_TOKEN))
                    .thenReturn("ROLE_UNKNOWN");

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();
        }
    }

    // =========================================================
    // MISSING OR INVALID AUTHORIZATION HEADER
    // =========================================================

    @Nested
    @DisplayName("Authorization header tests")
    class AuthorizationHeaderTests {

        @Test
        @DisplayName("Continue filter chain when Authorization header is missing")
        void doFilter_MissingHeader_ContinuesFilterChain() throws Exception {
            filter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());

            verifyNoInteractions(jwtService);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Reject request when Authorization header is not Bearer")
        void doFilter_NonBearerHeader_ReturnsUnauthorized() throws Exception {
            request.addHeader("Authorization", "Basic abc123");

            filter.doFilter(request, response, filterChain);

            assertUnauthorized();

            verifyNoInteractions(jwtService);
        }
    }
}
