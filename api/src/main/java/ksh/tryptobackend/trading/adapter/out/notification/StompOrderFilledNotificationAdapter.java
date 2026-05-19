package ksh.tryptobackend.trading.adapter.out.notification;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import ksh.tryptobackend.trading.adapter.out.notification.dto.OrderFilledStompPayload;
import ksh.tryptobackend.trading.application.port.out.OrderFilledNotificationPort;
import ksh.tryptobackend.trading.domain.vo.OrderFilledNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompOrderFilledNotificationAdapter implements OrderFilledNotificationPort {

    private static final String USER_DESTINATION = "/queue/events";

    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;
    private Timer matchToStomp;

    @PostConstruct
    void initMetrics() {
        matchToStomp = Timer.builder("trade.match.to.stomp")
                .description("매칭 결정 시점부터 STOMP convertAndSendToUser 직전까지의 e2e latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public void push(OrderFilledNotification notification) {
        try {
            OrderFilledStompPayload payload = OrderFilledStompPayload.from(notification);
            if (notification.matchedAt() != null) {
                long nanos = Duration.between(notification.matchedAt(), LocalDateTime.now()).toNanos();
                if (nanos >= 0) {
                    matchToStomp.record(nanos, TimeUnit.NANOSECONDS);
                }
            }
            messagingTemplate.convertAndSendToUser(
                    notification.userId().toString(), USER_DESTINATION, payload);
        } catch (Exception e) {
            log.warn(
                    "체결 알림 WebSocket 전송 실패: userId={}, orderId={}",
                    notification.userId(),
                    notification.orderId(),
                    e);
        }
    }
}
