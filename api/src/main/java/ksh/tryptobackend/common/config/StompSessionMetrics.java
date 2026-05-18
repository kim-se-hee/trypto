package ksh.tryptobackend.common.config;

import io.micrometer.core.instrument.Counter;
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
    private static final String CONNECT_COUNTER = "stomp.sessions.connect.total";
    private static final String DISCONNECT_COUNTER = "stomp.sessions.disconnect.total";

    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final Counter connectCounter;
    private final MeterRegistry meterRegistry;

    public StompSessionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(ACTIVE_GAUGE, activeSessions, AtomicInteger::get)
                .description("현재 활성 STOMP 세션 수")
                .register(meterRegistry);
        this.connectCounter =
                Counter.builder(CONNECT_COUNTER)
                        .description("STOMP 세션 연결 누계")
                        .register(meterRegistry);
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        activeSessions.incrementAndGet();
        connectCounter.increment();
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
        Counter.builder(DISCONNECT_COUNTER)
                .description("STOMP 세션 종료 누계 (reason 태그로 사유 분류)")
                .tag("reason", classify(status))
                .register(meterRegistry)
                .increment();
    }

    private String classify(CloseStatus status) {
        if (status == null) {
            return "unknown";
        }
        int code = status.getCode();
        return switch (code) {
            case 1000 -> "normal";
            case 1001 -> "going_away";
            case 1002 -> "protocol_error";
            case 1006 -> "no_close_frame";
            case 1008 -> "policy_violation";
            case 1011 -> "server_error";
            case 4500 -> "session_not_reliable";
            default -> "code_" + code;
        };
    }
}
