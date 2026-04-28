package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.FilledOrderCounts;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrphanOrder;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderQueryAdapter implements OrderQueryPort {

    private final JPAQueryFactory queryFactory;
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public List<OrphanOrder> findOrphanOrders(LocalDateTime threshold) {
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(OrphanOrder.class,
                o.id, o.walletId, o.exchangeCoinId, o.coinId, o.baseCoinId,
                o.exchangeName, o.marketSymbol,
                o.side, o.price, o.quantity, o.amount, o.createdAt))
            .from(o)
            .where(
                o.status.eq(OrderStatus.PENDING),
                o.createdAt.lt(threshold),
                o.price.isNotNull()
            )
            .fetch();
    }

    @Override
    public List<FilledOrder> findFilledByWalletAndCoin(Long walletId, Long coinId) {
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(FilledOrder.class,
                o.id, o.walletId, o.exchangeCoinId, o.side,
                o.amount, o.quantity, o.filledPrice, o.filledAt))
            .from(o)
            .where(
                o.walletId.eq(walletId),
                o.coinId.eq(coinId),
                o.status.eq(OrderStatus.FILLED)
            )
            .orderBy(o.filledAt.asc(), o.id.asc())
            .fetch();
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
    public List<FilledOrder> findFilledByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }

        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(FilledOrder.class,
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
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .selectOne()
            .from(o)
            .where(o.walletId.eq(walletId), o.status.eq(OrderStatus.FILLED))
            .fetchFirst() != null;
    }

    @Override
    public int countFilledByWalletId(Long walletId) {
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        Long count = queryFactory
            .select(o.count())
            .from(o)
            .where(o.walletId.eq(walletId), o.status.eq(OrderStatus.FILLED))
            .fetchOne();
        return count != null ? count.intValue() : 0;
    }

    @Override
    public FilledOrderCounts countFilledGroupByWalletId(List<Long> walletIds) {
        if (walletIds.isEmpty()) {
            return new FilledOrderCounts(Collections.emptyMap());
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

        Map<Long, Integer> countMap = results.stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(o.walletId),
                tuple -> tuple.get(o.walletId.count()).intValue()
            ));
        return new FilledOrderCounts(countMap);
    }

    @Override
    public List<FilledOrder> findFilledSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        QOrderJpaEntity o = QOrderJpaEntity.orderJpaEntity;
        return queryFactory
            .select(Projections.constructor(FilledOrder.class,
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
