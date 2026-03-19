package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExchangeQueryAdapter implements ExchangeQueryPort {

    private final ExchangeJpaRepository repository;
    private final CoinJpaRepository coinJpaRepository;

    @Override
    public Optional<Exchange> findExchangeDetailById(Long exchangeId) {
        return repository.findById(exchangeId)
            .map(ExchangeJpaEntity::toDomain);
    }

    @Override
    public Optional<ExchangeSummary> findExchangeSummaryById(Long exchangeId) {
        return repository.findById(exchangeId)
            .map(this::toExchangeSummary);
    }

    @Override
    public List<Long> findAllExchangeIds() {
        return repository.findAll().stream()
                .map(ExchangeJpaEntity::getId)
                .toList();
    }

    @Override
    public Map<Long, String> findNamesByIds(Set<Long> exchangeIds) {
        return repository.findAllById(exchangeIds).stream()
            .collect(Collectors.toMap(ExchangeJpaEntity::getId, ExchangeJpaEntity::getName));
    }

    @Override
    public boolean existsById(Long exchangeId) {
        return repository.existsById(exchangeId);
    }

    @Override
    public Optional<ExchangeSummary> findExchangeSummaryByName(String name) {
        return repository.findByName(name)
            .map(this::toExchangeSummary);
    }

    private ExchangeSummary toExchangeSummary(ExchangeJpaEntity entity) {
        String baseCurrencySymbol = coinJpaRepository.findById(entity.getBaseCurrencyCoinId())
            .map(CoinJpaEntity::getSymbol)
            .orElse("");
        return new ExchangeSummary(entity.getId(), entity.getName(), baseCurrencySymbol);
    }
}
