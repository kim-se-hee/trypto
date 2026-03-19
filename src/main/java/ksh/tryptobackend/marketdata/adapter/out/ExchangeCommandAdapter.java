package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCommandPort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExchangeCommandAdapter implements ExchangeCommandPort {

    private final ExchangeJpaRepository repository;

    @Override
    public Exchange save(String name, ExchangeMarketType marketType,
                         Long baseCurrencyCoinId, BigDecimal feeRate) {
        Long nextId = repository.count() + 1;
        ExchangeJpaEntity entity = new ExchangeJpaEntity(
                nextId, name, marketType, baseCurrencyCoinId, feeRate);
        return repository.save(entity).toDomain();
    }
}
