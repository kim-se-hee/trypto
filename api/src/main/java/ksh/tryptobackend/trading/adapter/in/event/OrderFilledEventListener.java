package ksh.tryptobackend.trading.adapter.in.event;

import ksh.tryptobackend.trading.application.port.in.SettleOrderUseCase;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderFilledEventListener {

    private final SettleOrderUseCase settleOrderUseCase;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void settle(OrderFilledEvent event) {
        settleOrderUseCase.settle(event);
    }
}
