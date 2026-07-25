package ksh.tryptobackend.marketdata.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.common.event.DomainEventPublisher;
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
    private final DomainEventPublisher domainEventPublisher;

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

    @Override
    public ExchangeCoin register(Long exchangeId, Long coinId, String displayName, String symbol) {
        return repository
                .findByExchangeIdAndCoinId(exchangeId, coinId)
                .map(entity -> relistExisting(entity, displayName, symbol))
                .orElseGet(() -> registerNewListing(exchangeId, coinId, displayName, symbol));
    }

    @Override
    public Optional<ExchangeCoin> suspend(Long exchangeId, Long coinId, String symbol) {
        return repository.findByExchangeIdAndCoinId(exchangeId, coinId).map(entity -> {
            ExchangeCoin exchangeCoin = entity.toDomain();
            exchangeCoin.suspend(symbol);
            entity.applyStatus(exchangeCoin.status());
            repository.save(entity);
            publishEvents(exchangeCoin);
            return exchangeCoin;
        });
    }

    private ExchangeCoin relistExisting(ExchangeCoinJpaEntity entity, String displayName, String symbol) {
        ExchangeCoin exchangeCoin = entity.toDomain();
        exchangeCoin.startTrading(displayName, symbol);
        entity.updateDisplayName(exchangeCoin.displayName());
        entity.applyStatus(exchangeCoin.status());
        repository.save(entity);
        publishEvents(exchangeCoin);
        return exchangeCoin;
    }

    private ExchangeCoin registerNewListing(Long exchangeId, Long coinId, String displayName, String symbol) {
        ExchangeCoin exchangeCoin = ExchangeCoin.newListing(exchangeId, coinId, displayName, symbol);
        ExchangeCoinJpaEntity saved = repository.save(new ExchangeCoinJpaEntity(exchangeId, coinId, displayName));
        exchangeCoin.assignId(saved.getId());
        publishEvents(exchangeCoin);
        return exchangeCoin;
    }

    private void publishEvents(ExchangeCoin exchangeCoin) {
        exchangeCoin.pullDomainEvents().forEach(domainEventPublisher::publish);
    }
}
