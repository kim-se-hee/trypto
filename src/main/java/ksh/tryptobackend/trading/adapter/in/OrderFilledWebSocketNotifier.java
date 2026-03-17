package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.trading.domain.vo.OrderFilledNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFilledWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderFilled(OrderFilledNotification notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                notification.userId().toString(),
                "/queue/events",
                notification.event());
        } catch (Exception e) {
            log.warn("체결 이벤트 전송 실패: userId={}, orderId={}",
                notification.userId(), notification.event().orderId(), e);
        }
    }
}
