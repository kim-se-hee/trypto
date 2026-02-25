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
public class MarketBuyOrderStrategy implements OrderPlacementStrategy {

    @Override
    public boolean supports(OrderType orderType, Side side) {
        return orderType == OrderType.MARKET && side == Side.BUY;
    }

    @Override
    public boolean requiresCurrentPrice() {
        return true;
    }

    @Override
    public Order createOrder(PlaceOrderCommand command, TradingVenue venue,
                             BigDecimal currentPrice, LocalDateTime now) {
        return Order.createMarketBuyOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), currentPrice, venue, now);
    }

    @Override
    public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
        return venue.baseCurrencyCoinId();
    }

    @Override
    public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
        return List.of(
            new BalanceChange.Deduct(venue.baseCurrencyCoinId(), order.getSettlementDebit()),
            new BalanceChange.Add(tradeCoinId, order.getQuantity().value())
        );
    }
}
