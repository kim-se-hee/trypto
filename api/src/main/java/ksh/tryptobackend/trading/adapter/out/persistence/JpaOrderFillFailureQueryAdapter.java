package ksh.tryptobackend.trading.adapter.out.persistence;

import java.util.List;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.OrderFillFailureJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.OrderFillFailureJpaRepository;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureQueryPort;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaOrderFillFailureQueryAdapter implements OrderFillFailureQueryPort {

    private final OrderFillFailureJpaRepository repository;

    @Override
    public List<OrderFillFailure> findUnresolved() {
        return repository.findByResolvedFalse().stream()
                .map(OrderFillFailureJpaEntity::toDomain)
                .toList();
    }
}
