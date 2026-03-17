package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheQueryPort;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PendingOrderCacheQueryAdapter implements PendingOrderCacheQueryPort {

    private final PendingOrderCacheCommandAdapter cacheStore;

    public PendingOrderCacheQueryAdapter(PendingOrderCacheCommandAdapter cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Override
    public List<PendingOrder> findMatchedOrders(Long exchangeCoinId, BigDecimal currentPrice) {
        return cacheStore.findMatchedOrders(exchangeCoinId, currentPrice);
    }
}
