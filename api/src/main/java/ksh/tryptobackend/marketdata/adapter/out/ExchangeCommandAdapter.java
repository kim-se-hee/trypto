package ksh.tryptobackend.marketdata.adapter.out;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCommandPort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCommandAdapter implements ExchangeCommandPort {

    private final ExchangeJpaRepository repository;

    @Override
    public Exchange save(String name, ExchangeMarketType marketType, Long baseCurrencyCoinId, BigDecimal feeRate) {
        ExchangeJpaEntity entity = repository
                .findByName(name)
                .map(existing -> {
                    existing.update(marketType, baseCurrencyCoinId, feeRate);
                    return existing;
                })
                .orElseGet(() ->
                        new ExchangeJpaEntity(repository.count() + 1, name, marketType, baseCurrencyCoinId, feeRate));
        return repository.save(entity).toDomain();
    }
}
