package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.SettleOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.vo.ExecutedFill;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettleOrderService implements SettleOrderUseCase {

    private final PositionCommandPort positionCommandPort;
    private final BalanceChangeApplier balanceChangeApplier;

    @Override
    @Transactional
    public void settle(OrderFilledEvent event) {
        Order order = event.order();
        TradingPair pair = event.tradingPair();

        balanceChangeApplier.applyAll(order.getWalletId(), order.planSettlementChanges(pair));

        Position position = positionCommandPort.getOrCreate(order.getWalletId(), pair.tradedCoinId());
        ExecutedFill executedFill = ExecutedFill.of(order.getSide(), order.getFilledPrice(), order.getQuantity());
        position.applyFill(executedFill, Price.of(event.currentPrice()));
        positionCommandPort.save(position);
    }
}
