package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.ExchangeCoinJpaRepository;
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
        ExchangeCoinJpaEntity entity = repository
                .findByExchangeIdAndCoinId(exchangeId, coinId)
                .map(existing -> {
                    existing.updateDisplayName(displayName);
                    return existing;
                })
                .orElseGet(() -> new ExchangeCoinJpaEntity(exchangeId, coinId, displayName));
        return repository.save(entity).toDomain();
    }
}
