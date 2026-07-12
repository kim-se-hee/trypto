package ksh.tryptobackend.user.adapter.out.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import ksh.tryptobackend.common.config.SessionProperties;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSessionCommandAdapter implements SessionCommandPort {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user-sessions:";

    private final StringRedisTemplate redisTemplate;
    private final SessionProperties sessionProperties;

    @Override
    public String create(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(sessionKey(sessionId), String.valueOf(userId), sessionProperties.getTtl());
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId);
        redisTemplate.expire(userSessionsKey(userId), sessionProperties.getTtl());
        return sessionId;
    }

    @Override
    public void delete(String sessionId) {
        String userId = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (userId != null) {
            redisTemplate.opsForSet().remove(userSessionsKey(Long.valueOf(userId)), sessionId);
        }
        redisTemplate.delete(sessionKey(sessionId));
    }

    @Override
    public void deleteAllOf(Long userId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        List<String> sessionKeys = sessionIds == null
                ? List.of()
                : sessionIds.stream().map(this::sessionKey).toList();
        redisTemplate.delete(sessionKeys);
        redisTemplate.delete(userSessionsKey(userId));
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
