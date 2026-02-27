package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.application.port.out.ExchangePort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeJpaPersistenceAdapter implements ExchangePort {

    private final ExchangeJpaRepository repository;

    @Override
    public Optional<Exchange> findById(Long exchangeId) {
        return repository.findById(exchangeId).map(ExchangeJpaEntity::toDomain);
    }
}
