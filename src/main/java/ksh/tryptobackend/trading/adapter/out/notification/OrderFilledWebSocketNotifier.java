package ksh.tryptobackend.trading.adapter.out.notification;

import ksh.tryptobackend.trading.adapter.out.dto.OrderFilledMessage;
import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;
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
    public void onOrderFilled(OrderFilledEvent event) {
        try {
            OrderFilledMessage message = OrderFilledMessage.from(event);
            messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/events",
                message);
        } catch (Exception e) {
            log.warn("체결 이벤트 전송 실패: userId={}, orderId={}",
                event.userId(), event.orderId(), e);
        }
    }
}
