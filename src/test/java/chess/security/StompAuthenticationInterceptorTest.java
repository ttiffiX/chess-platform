
package chess.security;

import chess.security.jwt.JwtService;
import chess.security.websocket.StompAuthenticationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthenticationInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthenticationInterceptor interceptor;

    private static final String VALID_TOKEN = "valid-token";
    private static final String USER_ID = "user-1";
    private static final String VALID_ROLE = "ROLE_USER";

    @BeforeEach
    void setUp() {
        // Không cần khởi tạo thêm vì đã dùng @InjectMocks.
    }

    // =========================================================
    // VALID TOKEN
    // =========================================================

    @Test
    void shouldAuthenticateWhenTokenIsValid() {
        mockValidToken();

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        result,
                        StompHeaderAccessor.class
                );

        assertNotNull(accessor);

        Principal principal = accessor.getUser();

        assertNotNull(principal);
        assertEquals(USER_ID, principal.getName());

        assertInstanceOf(Authentication.class, principal);

        Authentication authentication = (Authentication) principal;

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(VALID_ROLE)));

        verify(jwtService).validateAccessToken(VALID_TOKEN);
        verify(jwtService).getUserIdFromToken(VALID_TOKEN);
        verify(jwtService).getRoleFromToken(VALID_TOKEN);
    }

    // =========================================================
    // MISSING OR INVALID AUTHORIZATION HEADER
    // =========================================================

    @Test
    void shouldRejectConnectWhenAuthorizationHeaderIsMissing() {
        Message<?> message = createConnectMessage(null);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRejectConnectWhenBearerPrefixIsMissing() {
        Message<?> message = createConnectMessage(VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRejectConnectWhenAuthorizationHeaderIsBlank() {
        Message<?> message = createConnectMessage("   ");

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verifyNoInteractions(jwtService);
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void shouldRejectConnectWhenTokenIsInvalid() {
        when(jwtService.validateAccessToken("invalid-token"))
                .thenReturn(false);

        Message<?> message = createConnectMessage("Bearer invalid-token");

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verify(jwtService).validateAccessToken("invalid-token");
        verify(jwtService, never()).getUserIdFromToken(anyString());
        verify(jwtService, never()).getRoleFromToken(anyString());
    }

    @Test
    void shouldRejectConnectWhenBearerTokenIsEmpty() {
        Message<?> message = createConnectMessage("Bearer ");

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verifyNoInteractions(jwtService);
    }

    // =========================================================
    // INVALID USER ID
    // =========================================================

    @Test
    void shouldRejectConnectWhenUserIdIsNull() {
        when(jwtService.validateAccessToken(VALID_TOKEN))
                .thenReturn(true);

        when(jwtService.getUserIdFromToken(VALID_TOKEN))
                .thenReturn(null);

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verify(jwtService, never()).getRoleFromToken(anyString());
    }

    @Test
    void shouldRejectConnectWhenUserIdIsBlank() {
        when(jwtService.validateAccessToken(VALID_TOKEN))
                .thenReturn(true);

        when(jwtService.getUserIdFromToken(VALID_TOKEN))
                .thenReturn("   ");

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );

        verify(jwtService, never()).getRoleFromToken(anyString());
    }

    // =========================================================
    // INVALID ROLE
    // =========================================================

    @Test
    void shouldRejectConnectWhenRoleIsNull() {
        mockValidUserId();

        when(jwtService.getRoleFromToken(VALID_TOKEN))
                .thenReturn(null);

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void shouldRejectConnectWhenRoleIsBlank() {
        mockValidUserId();

        when(jwtService.getRoleFromToken(VALID_TOKEN))
                .thenReturn("   ");

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void shouldRejectConnectWhenRoleDoesNotExistInEnum() {
        mockValidUserId();

        when(jwtService.getRoleFromToken(VALID_TOKEN))
                .thenReturn("ROLE_UNKNOWN");

        Message<?> message = createConnectMessage("Bearer " + VALID_TOKEN);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void mockValidToken() {
        mockValidUserId();

        when(jwtService.getRoleFromToken(VALID_TOKEN))
                .thenReturn(VALID_ROLE);
    }

    private void mockValidUserId() {
        when(jwtService.validateAccessToken(VALID_TOKEN))
                .thenReturn(true);

        when(jwtService.getUserIdFromToken(VALID_TOKEN))
                .thenReturn(USER_ID);
    }

    private Message<?> createConnectMessage(String authorization) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(StompCommand.CONNECT);

        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }

        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }
}
