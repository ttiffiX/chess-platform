package chess.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "refresh_token:";

    public void save(String userId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, token, ttl);
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    }

    public void delete(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public boolean matches(String userId, String candidateToken) {
        String existingToken = get(userId);
        return existingToken != null && existingToken.equals(candidateToken);
    }
}