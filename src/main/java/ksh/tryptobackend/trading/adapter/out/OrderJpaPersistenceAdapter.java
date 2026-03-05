package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Projections;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.querydsl.core.Tuple;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderJpaPersistenceAdapter implements OrderPersistencePort, OrderQueryPort {

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

    @Override
    public List<OrderInfo> findFilledByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(OrderInfo.class,
                o.id, o.walletId, o.exchangeCoinId, o.side,
                o.amount, o.quantity, o.filledPrice, o.filledAt))
            .from(o)
            .where(
                o.id.in(orderIds),
                o.status.eq(OrderStatus.FILLED)
            )
            .fetch();
    }

    @Override
    public boolean existsFilledByWalletId(Long walletId) {
        return orderJpaRepository.existsByWalletIdAndStatus(walletId, OrderStatus.FILLED);
    }

    @Override
    public int countFilledByWalletId(Long walletId) {
        return orderJpaRepository.countByWalletIdAndStatus(walletId, OrderStatus.FILLED);
    }

    @Override
    public Map<Long, Integer> countFilledGroupByWalletId(List<Long> walletIds) {
        if (walletIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        List<Tuple> results = queryFactory
            .select(o.walletId, o.walletId.count())
            .from(o)
            .where(
                o.walletId.in(walletIds),
                o.status.eq(OrderStatus.FILLED)
            )
            .groupBy(o.walletId)
            .fetch();

        return results.stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(o.walletId),
                tuple -> tuple.get(o.walletId.count()).intValue()
            ));
    }

    @Override
    public List<OrderInfo> findFilledSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(OrderInfo.class,
                o.id, o.walletId, o.exchangeCoinId, o.side,
                o.amount, o.quantity, o.filledPrice, o.filledAt))
            .from(o)
            .where(
                o.walletId.eq(walletId),
                o.exchangeCoinId.eq(exchangeCoinId),
                o.side.eq(Side.SELL),
                o.status.eq(OrderStatus.FILLED),
                o.filledAt.goe(after)
            )
            .orderBy(o.filledAt.asc())
            .fetch();
    }
}
