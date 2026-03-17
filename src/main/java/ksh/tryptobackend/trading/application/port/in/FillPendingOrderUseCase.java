package ksh.tryptobackend.trading.application.port.in;

import java.math.BigDecimal;

public interface FillPendingOrderUseCase {

    void fillOrder(Long orderId, BigDecimal currentPrice);
}
