package ksh.tryptobackend.trading.application.strategy;

import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class LimitSellOrderStrategy implements OrderPlacementStrategy {

    @Override
    public boolean supports(OrderType orderType, Side side) {
        return orderType == OrderType.LIMIT && side == Side.SELL;
    }

    @Override
    public boolean requiresCurrentPrice() {
        return false;
    }

    @Override
    public Order createOrder(PlaceOrderCommand command, TradingVenue venue,
                             BigDecimal currentPrice, LocalDateTime now) {
        return Order.createLimitSellOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), command.price(), venue, now);
    }

    @Override
    public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
        return tradeCoinId;
    }

    @Override
    public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
        return List.of(
            new BalanceChange.Lock(tradeCoinId, order.getQuantity().value())
        );
    }
}
