package ksh.tryptobackend.trading.domain.vo;

import ksh.tryptobackend.trading.domain.model.Order;

import java.math.BigDecimal;

public record OrderPlacedEvent(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    Long coinId,
    Side side,
    OrderType orderType,
    Quantity quantity,
    BigDecimal price,
    BigDecimal filledPrice,
    BigDecimal currentPrice
) {

    public static OrderPlacedEvent of(Order order, TradingContext ctx) {
        return new OrderPlacedEvent(
            order.getId(), order.getWalletId(), order.getExchangeCoinId(),
            ctx.coinId(), order.getSide(), order.getOrderType(),
            order.getQuantity(), order.getPrice(),
            order.getFilledPrice(), ctx.currentPrice());
    }

    public boolean isMarketOrder() {
        return orderType == OrderType.MARKET;
    }

    public boolean isLimitOrder() {
        return orderType == OrderType.LIMIT;
    }
}
