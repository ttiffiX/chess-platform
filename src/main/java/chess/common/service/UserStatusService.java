package chess.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final StringRedisTemplate redisTemplate;
    private static final String BAN_KEY_PREFIX = "user:banned:";

    public boolean isBanned(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BAN_KEY_PREFIX + userId));
    }

    public void ban(String userId, Duration ttl) {
        redisTemplate.opsForValue().set(BAN_KEY_PREFIX + userId, "1", ttl);
    }

    public void unban(String userId) {
        redisTemplate.delete(BAN_KEY_PREFIX + userId);
    }
}