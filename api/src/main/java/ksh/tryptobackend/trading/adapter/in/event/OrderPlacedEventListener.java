package ksh.tryptobackend.trading.adapter.in.event;

import ksh.tryptobackend.trading.application.port.in.RecordOrderViolationsUseCase;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedEventListener {

    private final RecordOrderViolationsUseCase recordOrderViolationsUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recordViolations(OrderPlacedEvent event) {
        try {
            recordOrderViolationsUseCase.record(event);
        } catch (Exception e) {
            log.warn("주문 룰 위반 기록 실패 orderId={}", event.orderId(), e);
        }
    }
}
