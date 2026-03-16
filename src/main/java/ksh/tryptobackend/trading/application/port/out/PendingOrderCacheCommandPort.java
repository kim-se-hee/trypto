package ksh.tryptobackend.trading.application.port.out;

public interface PendingOrderCacheCommandPort {

    void remove(Long exchangeCoinId, Long orderId);
}
