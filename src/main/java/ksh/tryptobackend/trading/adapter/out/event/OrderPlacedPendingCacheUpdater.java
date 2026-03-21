package ksh.tryptobackend.trading.adapter.out.event;

import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.domain.vo.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderPlacedPendingCacheUpdater {

    private final PendingOrderCacheCommandPort pendingOrderCacheCommandPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (!event.isLimitOrder()) {
            return;
        }

        pendingOrderCacheCommandPort.add(
            new PendingOrder(event.orderId(), event.exchangeCoinId(), event.side(), event.price()));
    }
}
