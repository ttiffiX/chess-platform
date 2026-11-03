
package chess.security.websocket;

import chess.feature.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern GAME_SEND_PATTERN =
            Pattern.compile("^/app/game/([^/]+)/.+$");

    private static final Pattern GAME_SUBSCRIBE_PATTERN =
            Pattern.compile("^/topic/game/([^/]+)$");

    private static final Set<String> ALLOWED_SUBSCRIPTIONS = Set.of(
            "/user/queue/match-status",
            "/user/queue/match-found"
    );

    private final GameService gameService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            throw new BadCredentialsException("Invalid STOMP message");
        }

        StompCommand command = accessor.getCommand();

        if (command == null || command == StompCommand.CONNECT) {
            return message;
        }

        Principal principal = accessor.getUser();

        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new AccessDeniedException("Unauthenticated STOMP session");
        }

        String userId = principal.getName();
        String destination = accessor.getDestination();

        if (command == StompCommand.SEND) {
            authorizeSend(destination, userId);
        }

        if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscribe(destination, userId);
        }

        return message;
    }

    private void authorizeSend(String destination, String userId) {
        if (!StringUtils.hasText(destination)) {
            throw new AccessDeniedException("Missing destination");
        }

        // Không cho client gửi trực tiếp vào broker destinations.
        if (destination.startsWith("/topic/")
                || destination.startsWith("/queue/")
                || destination.startsWith("/user/")) {
            throw new AccessDeniedException("SEND destination is not allowed");
        }

        // Các message thuộc game phải đến từ thành viên của game.
        Matcher matcher = GAME_SEND_PATTERN.matcher(destination);

        if (matcher.matches()) {
            String gameId = matcher.group(1);

            if (!gameService.isPlayerInGame(gameId, userId)) {
                throw new AccessDeniedException(
                        "You are not a participant of this game"
                );
            }
        }

        // Các destination khác tiếp tục được Spring dispatch.
        // Nếu không có @MessageMapping tương ứng, Spring xử lý message không có handler.
    }

    private void authorizeSubscribe(String destination, String userId) {
        if (!StringUtils.hasText(destination)) {
            throw new AccessDeniedException("Missing destination");
        }

        // Queue cá nhân của matchmaking.
        if (ALLOWED_SUBSCRIPTIONS.contains(destination)) {
            return;
        }

        // Topic game: phải là thành viên của game.
        Matcher matcher = GAME_SUBSCRIBE_PATTERN.matcher(destination);

        if (matcher.matches()) {
            String gameId = matcher.group(1);

            if (!gameService.isPlayerInGame(gameId, userId)) {
                throw new AccessDeniedException(
                        "You are not allowed to subscribe to this game"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "SUBSCRIBE destination is not allowed"
        );
    }
}
