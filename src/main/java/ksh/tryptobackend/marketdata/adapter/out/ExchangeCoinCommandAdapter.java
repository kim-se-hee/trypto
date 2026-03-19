package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinCommandPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCoinCommandAdapter implements ExchangeCoinCommandPort {

    private final ExchangeCoinJpaRepository repository;

    @Override
    public ExchangeCoin save(Long exchangeId, Long coinId, String displayName) {
        ExchangeCoinJpaEntity entity = new ExchangeCoinJpaEntity(exchangeId, coinId, displayName);
        ExchangeCoinJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }
}
