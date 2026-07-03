package ksh.tryptobackend.trading.domain.event;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.TradingPair;

public record OrderFilledEvent(Order order, TradingPair tradingPair, BigDecimal currentPrice) {

    public static OrderFilledEvent of(Order order, MarketInfo ctx) {
        return new OrderFilledEvent(order, ctx.tradingPair(), ctx.currentPrice().value());
    }
}
