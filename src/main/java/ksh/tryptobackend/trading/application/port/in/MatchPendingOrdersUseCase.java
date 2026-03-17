package ksh.tryptobackend.trading.application.port.in;

import java.math.BigDecimal;

public interface MatchPendingOrdersUseCase {

    void matchOrders(String exchange, String symbol, BigDecimal currentPrice);
}
