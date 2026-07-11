package ksh.tryptobackend.user.adapter.out;

import java.util.UUID;
import ksh.tryptobackend.common.config.SessionProperties;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionCommandAdapter implements SessionCommandPort {

    private static final String SESSION_KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final SessionProperties sessionProperties;

    @Override
    public String create(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate
                .opsForValue()
                .set(
                        SESSION_KEY_PREFIX + sessionId,
                        String.valueOf(userId),
                        sessionProperties.getTtl());
        return sessionId;
    }
}
