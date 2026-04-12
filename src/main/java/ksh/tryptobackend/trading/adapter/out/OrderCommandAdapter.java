package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderJpaRepository orderJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        OrderJpaEntity saved = orderJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findById(orderId)
            .map(OrderJpaEntity::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return orderJpaRepository.findByIdempotencyKey(idempotencyKey)
            .map(OrderJpaEntity::toDomain);
    }

    @Override
    public long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to) {
        return orderJpaRepository.countByWalletIdAndCreatedAtBetween(walletId, from, to);
    }

    @Override
    public boolean fillOrder(Long orderId, LocalDateTime filledAt) {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;
        long count = queryFactory.update(order)
            .set(order.status, OrderStatus.FILLED)
            .set(order.filledAt, filledAt)
            .where(order.id.eq(orderId)
                .and(order.status.eq(OrderStatus.PENDING)))
            .execute();
        return count > 0;
    }

    @Override
    public boolean cancelOrder(Long orderId) {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;
        long count = queryFactory.update(order)
            .set(order.status, OrderStatus.CANCELLED)
            .where(order.id.eq(orderId)
                .and(order.status.eq(OrderStatus.PENDING)))
            .execute();
        return count > 0;
    }
}
