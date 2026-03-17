package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.adapter.out.entity.OrderFillFailureJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderFillFailureJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureQueryPort;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFillFailureQueryAdapter implements OrderFillFailureQueryPort {

    private final OrderFillFailureJpaRepository repository;

    @Override
    public List<OrderFillFailure> findUnresolved() {
        return repository.findByResolvedFalse().stream()
            .map(OrderFillFailureJpaEntity::toDomain)
            .toList();
    }
}
