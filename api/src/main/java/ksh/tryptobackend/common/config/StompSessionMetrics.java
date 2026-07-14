package ksh.tryptobackend.common.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class StompSessionMetrics {

    private static final String ACTIVE_GAUGE = "stomp.sessions.active";

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public StompSessionMetrics(MeterRegistry meterRegistry) {
        Gauge.builder(ACTIVE_GAUGE, activeSessions, AtomicInteger::get)
                .description("현재 활성 STOMP 세션 수")
                .register(meterRegistry);
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        activeSessions.incrementAndGet();
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        activeSessions.decrementAndGet();
        CloseStatus status = event.getCloseStatus();
        if (status != null && status.getCode() != 1000 && status.getCode() != 1001) {
            log.warn(
                    "STOMP session closed sessionId={} code={} reason='{}'",
                    event.getSessionId(),
                    status.getCode(),
                    status.getReason());
        }
    }
}
