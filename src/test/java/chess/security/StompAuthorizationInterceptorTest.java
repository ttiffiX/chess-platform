package chess.security;

import chess.feature.game.service.GameService;
import chess.security.websocket.StompAuthorizationInterceptor;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthorizationInterceptorTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private StompAuthorizationInterceptor interceptor;

    private static final String USER_ID = "user-1";
    private static final String GAME_ID = "game-1";

    @Test
    void shouldAllowSendWhenPlayerBelongsToGame() {
        when(gameService.isPlayerInGame(GAME_ID, USER_ID))
                .thenReturn(true);

        Message<?> message = createMessage(
                StompCommand.SEND,
                "/app/game/" + GAME_ID + "/move",
                USER_ID
        );

        Message<?> result = interceptor.preSend(
                message,
                mock(MessageChannel.class)
        );

        assertNotNull(result);
        verify(gameService).isPlayerInGame(GAME_ID, USER_ID);
    }

    @Test
    void shouldRejectSendWhenPlayerDoesNotBelongToGame() {
        when(gameService.isPlayerInGame(GAME_ID, USER_ID))
                .thenReturn(false);

        Message<?> message = createMessage(
                StompCommand.SEND,
                "/app/game/" + GAME_ID + "/move",
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void shouldRejectSendWhenUserIsUnauthenticated() {
        Message<?> message = createMessage(
                StompCommand.SEND,
                "/app/game/" + GAME_ID + "/move",
                null
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void shouldAllowSubscribeToGameTopicWhenPlayerBelongsToGame() {
        when(gameService.isPlayerInGame(GAME_ID, USER_ID))
                .thenReturn(true);

        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                "/topic/game/" + GAME_ID,
                USER_ID
        );

        Message<?> result = interceptor.preSend(
                message,
                mock(MessageChannel.class)
        );

        assertNotNull(result);
    }

    @Test
    void shouldRejectSubscribeToGameTopicWhenPlayerDoesNotBelongToGame() {
        when(gameService.isPlayerInGame(GAME_ID, USER_ID))
                .thenReturn(false);

        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                "/topic/game/" + GAME_ID,
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void shouldAllowSubscribeToMatchStatusQueue() {
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/match-status",
                USER_ID
        );

        Message<?> result = interceptor.preSend(
                message,
                mock(MessageChannel.class)
        );

        assertNotNull(result);
    }

    @Test
    void shouldRejectSubscribeToUnknownDestination() {
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                "/topic/something-else",
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    @Test
    void shouldRejectSendToBrokerDestination() {
        Message<?> message = createMessage(
                StompCommand.SEND,
                "/topic/game/" + GAME_ID,
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );
    }

    private Message<?> createMessage(
            StompCommand command,
            String destination,
            String userId
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);

        if (userId != null) {
            accessor.setUser(
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    )
            );
        }

        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }

    @Test
    void shouldRejectSendWhenDestinationIsMissing() {
        Message<?> message = createMessage(
                StompCommand.SEND,
                null,
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );

        verifyNoInteractions(gameService);
    }

    @Test
    void shouldRejectSubscribeWhenDestinationIsMissing() {
        Message<?> message = createMessage(
                StompCommand.SUBSCRIBE,
                null,
                USER_ID
        );

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class))
        );

        verifyNoInteractions(gameService);
    }

    @Test
    void shouldAllowSendToOtherApplicationDestination() {
        Message<?> message = createMessage(
                StompCommand.SEND,
                "/app/matchmaking/join",
                USER_ID
        );

        Message<?> result = interceptor.preSend(
                message,
                mock(MessageChannel.class)
        );

        assertNotNull(result);
        verifyNoInteractions(gameService);
    }
}
