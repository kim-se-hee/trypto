package ksh.tryptobackend.common.web.auth;

import java.util.Optional;
import ksh.tryptobackend.common.config.SessionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSessionReader implements SessionReader {

    private static final String SESSION_KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final SessionProperties sessionProperties;

    @Override
    public Optional<Long> findUserId(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return Optional.empty();
        }
        redisTemplate.expire(key, sessionProperties.getTtl());
        return Optional.of(Long.valueOf(userId));
    }
}
