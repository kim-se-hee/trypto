package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.PendingOrder;

import java.math.BigDecimal;
import java.util.List;

public interface PendingOrderCacheCommandPort {

    void add(PendingOrder pendingOrder);

    void remove(Long exchangeCoinId, Long orderId);

    List<PendingOrder> findMatchedOrders(Long exchangeCoinId, BigDecimal currentPrice);

    void addAll(List<PendingOrder> pendingOrders);
}
