package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeQueryAdapter implements ExchangeQueryPort {

    private final ExchangeJpaRepository repository;
    private final CoinJpaRepository coinJpaRepository;

    @Override
    public Optional<ExchangeDetail> findExchangeDetailById(Long exchangeId) {
        return repository.findById(exchangeId)
            .map(entity -> new ExchangeDetail(entity.getName(), entity.getBaseCurrencyCoinId(),
                entity.getMarketType() == ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType.DOMESTIC,
                entity.getFeeRate()));
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
