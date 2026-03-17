package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.PendingOrder;

import java.util.List;

public interface PendingOrderCacheCommandPort {

    void add(PendingOrder pendingOrder);

    void remove(Long exchangeCoinId, Long orderId);

    void addAll(List<PendingOrder> pendingOrders);
}
