package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangePort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeSummary;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeJpaPersistenceAdapter implements ExchangePort, ExchangeQueryPort {

    private final ExchangeJpaRepository repository;
    private final CoinJpaRepository coinJpaRepository;

    @Override
    public Optional<Exchange> findById(Long exchangeId) {
        return repository.findById(exchangeId).map(ExchangeJpaEntity::toDomain);
    }

    @Override
    public Optional<ExchangeDetail> findExchangeDetailById(Long exchangeId) {
        return repository.findById(exchangeId)
            .map(entity -> new ExchangeDetail(entity.getBaseCurrencyCoinId(), entity.getMarketType()));
    }

    @Override
    public Optional<ExchangeSummary> findExchangeSummaryById(Long exchangeId) {
        return repository.findById(exchangeId)
            .map(this::toExchangeSummary);
    }

    private ExchangeSummary toExchangeSummary(ExchangeJpaEntity entity) {
        String baseCurrencySymbol = coinJpaRepository.findById(entity.getBaseCurrencyCoinId())
            .map(CoinJpaEntity::getSymbol)
            .orElse("");
        return new ExchangeSummary(entity.getId(), entity.getName(), baseCurrencySymbol);
    }
}
