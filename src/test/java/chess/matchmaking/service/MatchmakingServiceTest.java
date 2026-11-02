package chess.matchmaking.service;

import chess.exception.AppException;
import chess.feature.game.dto.request.CreateGameRequest;
import chess.feature.game.service.GameService;
import chess.feature.game.session.GameSession;
import chess.feature.matchmaking.dto.MatchRequest;
import chess.feature.matchmaking.dto.MatchResult;
import chess.feature.matchmaking.enums.MatchmakingTimeControlPreset;
import chess.feature.matchmaking.service.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private GameService gameService;

    @Mock
    private GameSession gameSession;

    @InjectMocks
    private MatchmakingService matchmakingService;

    @Test
    void registerPlayer_shouldAddPlayerToQueue_whenNoOpponentExists() {
        // Arrange
        MatchRequest request = request(MatchmakingTimeControlPreset.BLITZ_3_0);

        // Act
        MatchResult result = matchmakingService.registerPlayer("player-1", request);

        // Assert
        assertFalse(result.matched());
        assertNull(result.opponentId());
        assertNull(result.session());

        verify(gameService, never()).createGame(any());
    }

    @Test
    void registerPlayer_shouldMatchPlayers_whenSamePreset() {
        // Arrange
        MatchRequest request = request(MatchmakingTimeControlPreset.BLITZ_3_0);

        when(gameService.createGame(any(CreateGameRequest.class)))
                .thenReturn(gameSession);

        // Act
        MatchResult firstResult =
                matchmakingService.registerPlayer("player-1", request);

        MatchResult secondResult =
                matchmakingService.registerPlayer("player-2", request);

        // Assert
        assertFalse(firstResult.matched());

        assertTrue(secondResult.matched());
        assertEquals("player-1", secondResult.opponentId());
        assertSame(gameSession, secondResult.session());

        ArgumentCaptor<CreateGameRequest> captor =
                ArgumentCaptor.forClass(CreateGameRequest.class);

        verify(gameService).createGame(captor.capture());

        CreateGameRequest createRequest = captor.getValue();

        assertEquals(180L, createRequest.baseTimeSeconds());
        assertEquals(0L, createRequest.incrementSeconds());
        assertEquals("player-1", createRequest.player1Id());
        assertEquals("player-2", createRequest.player2Id());
    }

    @Test
    void registerPlayer_shouldNotMatchPlayers_whenPresetsAreDifferent() {
        // Arrange
        MatchRequest blitzRequest =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        MatchRequest bulletRequest =
                request(MatchmakingTimeControlPreset.BULLET_1_0);

        // Act
        MatchResult firstResult =
                matchmakingService.registerPlayer("player-1", blitzRequest);

        MatchResult secondResult =
                matchmakingService.registerPlayer("player-2", bulletRequest);

        // Assert
        assertFalse(firstResult.matched());
        assertFalse(secondResult.matched());

        verify(gameService, never()).createGame(any());
    }

    @Test
    void registerPlayer_shouldKeepQueuesSeparatedByPreset() {
        // Arrange
        MatchRequest blitzRequest =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        MatchRequest bulletRequest =
                request(MatchmakingTimeControlPreset.BULLET_1_0);

        when(gameService.createGame(any(CreateGameRequest.class)))
                .thenReturn(gameSession);

        // Act
        matchmakingService.registerPlayer("player-1", blitzRequest);
        matchmakingService.registerPlayer("player-2", bulletRequest);

        // Player 3 sử dụng preset của player 1.
        MatchResult result =
                matchmakingService.registerPlayer("player-3", blitzRequest);

        // Assert
        assertTrue(result.matched());
        assertEquals("player-1", result.opponentId());

        verify(gameService).createGame(any(CreateGameRequest.class));
    }

    @Test
    void registerPlayer_shouldThrowException_whenPlayerAlreadyInQueue() {
        // Arrange
        MatchRequest request =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        matchmakingService.registerPlayer("player-1", request);

        // Act & Assert
        assertThrows(
                AppException.class,
                () -> matchmakingService.registerPlayer("player-1", request)
        );

        // Player 2 vẫn phải ghép được với đúng ticket của player 1.
        when(gameService.createGame(any(CreateGameRequest.class)))
                .thenReturn(gameSession);

        MatchResult result =
                matchmakingService.registerPlayer("player-2", request);

        assertTrue(result.matched());
        assertEquals("player-1", result.opponentId());

        verify(gameService, times(1)).createGame(any(CreateGameRequest.class));
    }

    @Test
    void registerPlayer_shouldThrowException_whenPlayerAlreadyHasActiveGame() {
        // Arrange
        MatchRequest request =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        when(gameService.hasActiveGame("player-1"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                AppException.class,
                () -> matchmakingService.registerPlayer("player-1", request)
        );

        verify(gameService, never()).createGame(any());
    }

    @Test
    void removePlayer_shouldRemovePlayerFromQueue() {
        // Arrange
        MatchRequest request =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        matchmakingService.registerPlayer("player-1", request);

        // Act
        boolean removed = matchmakingService.removePlayer("player-1");

        // Assert
        assertTrue(removed);

        // Player 2 không được ghép với player đã cancel.
        MatchResult result =
                matchmakingService.registerPlayer("player-2", request);

        assertFalse(result.matched());

        verify(gameService, never()).createGame(any());
    }

    @Test
    void removePlayer_shouldReturnFalse_whenPlayerIsNotInQueue() {
        assertFalse(matchmakingService.removePlayer("unknown-player"));
    }

    @Test
    void registerPlayer_shouldRestoreOpponentToQueue_whenCreateGameFails() {
        // Arrange
        MatchRequest request =
                request(MatchmakingTimeControlPreset.BLITZ_3_0);

        matchmakingService.registerPlayer("player-1", request);

        when(gameService.createGame(any(CreateGameRequest.class)))
                .thenThrow(new RuntimeException("Game creation failed"))
                .thenReturn(gameSession);

        // Act
        assertThrows(
                RuntimeException.class,
                () -> matchmakingService.registerPlayer("player-2", request)
        );

        // Player 3 phải ghép được với player 1, không phải player 2.
        MatchResult result =
                matchmakingService.registerPlayer("player-3", request);

        // Assert
        assertTrue(result.matched());
        assertEquals("player-1", result.opponentId());

        ArgumentCaptor<CreateGameRequest> captor =
                ArgumentCaptor.forClass(CreateGameRequest.class);

        verify(gameService, times(2)).createGame(captor.capture());

        CreateGameRequest successfulRequest =
                captor.getAllValues().get(1);

        assertEquals("player-1", successfulRequest.player1Id());
        assertEquals("player-3", successfulRequest.player2Id());
    }

    @Test
    void removePlayer_shouldReturnFalse_whenUserIdIsNullOrBlank() {
        assertFalse(matchmakingService.removePlayer(null));
        assertFalse(matchmakingService.removePlayer(" "));
    }

    private MatchRequest request(MatchmakingTimeControlPreset preset) {
        return new MatchRequest(preset);
    }
}