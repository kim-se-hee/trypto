package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderJpaPersistenceAdapter implements OrderPersistencePort {

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
    public List<Order> findByCursor(Long walletId, Long exchangeCoinId, Side side,
                                    OrderStatus status, Long cursorOrderId, int size) {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

        return queryFactory
            .selectFrom(order)
            .where(
                order.walletId.eq(walletId),
                exchangeCoinIdEq(order, exchangeCoinId),
                sideEq(order, side),
                statusEq(order, status),
                cursorLt(order, cursorOrderId)
            )
            .orderBy(order.id.desc())
            .limit(size)
            .fetch()
            .stream()
            .map(OrderJpaEntity::toDomain)
            .toList();
    }

    @Override
    public long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to) {
        return orderJpaRepository.countByWalletIdAndCreatedAtBetween(walletId, from, to);
    }

    private BooleanExpression exchangeCoinIdEq(QOrderJpaEntity order, Long exchangeCoinId) {
        return exchangeCoinId != null ? order.exchangeCoinId.eq(exchangeCoinId) : null;
    }

    private BooleanExpression sideEq(QOrderJpaEntity order, Side side) {
        return side != null ? order.side.eq(side) : null;
    }

    private BooleanExpression statusEq(QOrderJpaEntity order, OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    private BooleanExpression cursorLt(QOrderJpaEntity order, Long cursorOrderId) {
        return cursorOrderId != null ? order.id.lt(cursorOrderId) : null;
    }
}
