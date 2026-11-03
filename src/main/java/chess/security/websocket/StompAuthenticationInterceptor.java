package chess.security.websocket;

import chess.feature.user.enums.Role;
import chess.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            throw new BadCredentialsException("Invalid STOMP message");
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (command != null && accessor.getUser() == null) {
            throw new BadCredentialsException("Unauthenticated STOMP session");
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");

        if (!StringUtils.hasText(authorization)
                || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing or invalid Authorization header");
        }

        String token = authorization.substring(7).trim();

        if (!StringUtils.hasText(token)
                || !jwtService.validateAccessToken(token)) {
            throw new BadCredentialsException("Invalid or expired access token");
        }

        String userId = jwtService.getUserIdFromToken(token);

        if (!StringUtils.hasText(userId)) {
            throw new BadCredentialsException("Invalid user identity");
        }

        String role = jwtService.getRoleFromToken(token);

        if (!Role.isValid(role)) {
            throw new BadCredentialsException("Invalid user role");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

        accessor.setUser(authentication);
    }
}