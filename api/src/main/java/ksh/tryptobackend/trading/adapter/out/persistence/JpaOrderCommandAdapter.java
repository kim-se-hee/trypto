package ksh.tryptobackend.trading.adapter.out.persistence;

import ksh.tryptobackend.common.event.DomainEventPublisher;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaOrderCommandAdapter implements OrderCommandPort {

    private final OrderJpaRepository orderJpaRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        OrderJpaEntity saved = orderJpaRepository.save(entity);
        if (order.getId() == null) {
            order.assignId(saved.getId());
        }
        order.pullDomainEvents().forEach(domainEventPublisher::publish);
        return order;
    }
}
