package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PendingOrderCacheCommandAdapter implements PendingOrderCacheCommandPort {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<PendingOrder>> cache =
        new ConcurrentHashMap<>();

    @Override
    public void add(PendingOrder pendingOrder) {
        cache.computeIfAbsent(pendingOrder.exchangeCoinId(), k -> new CopyOnWriteArrayList<>())
            .add(pendingOrder);
    }

    @Override
    public void remove(Long exchangeCoinId, Long orderId) {
        CopyOnWriteArrayList<PendingOrder> orders = cache.get(exchangeCoinId);
        if (orders != null) {
            orders.removeIf(o -> o.orderId().equals(orderId));
        }
    }

    @Override
    public void addAll(List<PendingOrder> pendingOrders) {
        for (PendingOrder order : pendingOrders) {
            add(order);
        }
    }

    List<PendingOrder> findMatchedOrders(Long exchangeCoinId, BigDecimal currentPrice) {
        CopyOnWriteArrayList<PendingOrder> orders = cache.get(exchangeCoinId);
        if (orders == null) {
            return Collections.emptyList();
        }
        return orders.stream()
            .filter(o -> o.matches(currentPrice))
            .toList();
    }
}
