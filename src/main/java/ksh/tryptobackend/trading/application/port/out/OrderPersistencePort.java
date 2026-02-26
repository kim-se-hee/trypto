package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderPersistencePort {

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByCursor(Long walletId, Long exchangeCoinId, Side side,
                             OrderStatus status, Long cursorOrderId, int size);

    long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to);
}
