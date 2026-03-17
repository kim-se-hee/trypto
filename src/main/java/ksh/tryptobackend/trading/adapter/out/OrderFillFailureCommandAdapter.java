package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.adapter.out.entity.OrderFillFailureJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderFillFailureJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureCommandPort;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderFillFailureCommandAdapter implements OrderFillFailureCommandPort {

    private final OrderFillFailureJpaRepository repository;

    @Override
    public OrderFillFailure save(OrderFillFailure failure) {
        OrderFillFailureJpaEntity entity = OrderFillFailureJpaEntity.fromDomain(failure);
        return repository.save(entity).toDomain();
    }
}
