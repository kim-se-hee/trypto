package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.PendingOrder;

import java.math.BigDecimal;
import java.util.List;

public interface PendingOrderCacheQueryPort {

    List<PendingOrder> findMatchedOrders(Long exchangeCoinId, BigDecimal currentPrice);
}
