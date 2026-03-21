package ksh.tryptobackend.trading.adapter.out.event;

import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.vo.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderPlacedHoldingUpdater {

    private final HoldingCommandPort holdingCommandPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (!event.isMarketOrder()) {
            return;
        }

        Holding holding = holdingCommandPort
            .findByWalletIdAndCoinId(event.walletId(), event.coinId())
            .orElseGet(() -> Holding.empty(event.walletId(), event.coinId()));
        holding.applyOrder(event.side(), event.filledPrice(), event.quantity().value(), event.currentPrice());
        holdingCommandPort.save(holding);
    }
}
