package ksh.tryptobackend.trading.adapter.out.notification;

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

    @Override
    public void push(OrderFilledNotification notification) {
        try {
            OrderFilledStompPayload payload = OrderFilledStompPayload.from(notification);
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
