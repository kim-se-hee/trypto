package ksh.tryptobackend.trading.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyCommandPort;
import ksh.tryptobackend.common.idempotency.IdempotencyResourceType;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final IdempotencyKeyCommandPort idempotencyKeyCommandPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;
    private final BalanceChangeApplier balanceChangeApplier;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand cmd) {
        LocalDateTime now = LocalDateTime.now(clock);
        idempotencyKeyCommandPort.preempt(cmd.idempotencyKey(), IdempotencyResourceType.PLACE_ORDER, now);

        MarketInfo marketInfo = marketQueryPort.findByExchangeCoinId(cmd.exchangeCoinId());
        Order order = Order.create(cmd, marketInfo, now);

        BalanceChange reservation = order.planReservation(marketInfo.tradingPair());
        balanceChangeApplier.apply(order.getWalletId(), reservation);

        Order saved = orderCommandPort.save(order);
        idempotencyKeyCommandPort.linkResource(cmd.idempotencyKey(), saved.getId());
        return saved;
    }
}
