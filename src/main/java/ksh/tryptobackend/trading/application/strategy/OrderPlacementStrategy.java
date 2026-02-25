package ksh.tryptobackend.trading.application.strategy;

import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderPlacementStrategy {

    boolean supports(OrderType orderType, Side side);

    boolean requiresCurrentPrice();

    Order createOrder(PlaceOrderCommand command, TradingVenue venue,
                      BigDecimal currentPrice, LocalDateTime now);

    Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId);

    List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId);
}
