package ksh.tryptobackend.user.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import ksh.tryptobackend.common.config.SessionProperties;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSessionCommandAdapter implements SessionCommandPort {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user-sessions:";

    private static final RedisScript<Long> CREATE_SCRIPT = RedisScript.of("""
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            redis.call('SADD', KEYS[2], ARGV[2])
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            return 1
            """, Long.class);

    private static final RedisScript<Long> DELETE_SCRIPT = RedisScript.of("""
            local userId = redis.call('GET', KEYS[1])
            if userId then
                redis.call('SREM', ARGV[2] .. userId, ARGV[1])
            end
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private static final RedisScript<Long> DELETE_ALL_SCRIPT = RedisScript.of("""
            local sessionIds = redis.call('SMEMBERS', KEYS[1])
            for i = 1, #sessionIds do
                redis.call('DEL', ARGV[1] .. sessionIds[i])
            end
            redis.call('DEL', KEYS[1])
            return #sessionIds
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SessionProperties sessionProperties;

    @Override
    public String create(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.execute(
                CREATE_SCRIPT,
                List.of(sessionKey(sessionId), userSessionsKey(userId)),
                String.valueOf(userId),
                sessionId,
                ttlSeconds());
        return sessionId;
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.execute(DELETE_SCRIPT, List.of(sessionKey(sessionId)), sessionId, USER_SESSIONS_KEY_PREFIX);
    }

    @Override
    public void deleteAllOf(Long userId) {
        redisTemplate.execute(DELETE_ALL_SCRIPT, List.of(userSessionsKey(userId)), SESSION_KEY_PREFIX);
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }

    private String ttlSeconds() {
        return String.valueOf(sessionProperties.getTtl().toSeconds());
    }
}
