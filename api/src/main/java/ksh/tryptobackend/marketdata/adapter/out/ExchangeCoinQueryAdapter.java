package ksh.tryptobackend.marketdata.adapter.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinIdMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCoinQueryAdapter implements ExchangeCoinQueryPort {

    private final ExchangeCoinJpaRepository repository;

    @Override
    public Optional<ExchangeCoin> findById(Long exchangeCoinId) {
        return repository.findById(exchangeCoinId).map(ExchangeCoinJpaEntity::toDomain);
    }

    @Override
    public boolean existsByExchangeIdAndCoinId(Long exchangeId, Long coinId) {
        return repository.existsByExchangeIdAndCoinId(exchangeId, coinId);
    }

    @Override
    public ExchangeCoinIdMap findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds) {
        Map<Long, Long> map =
                repository.findByExchangeIdAndCoinIdIn(exchangeId, coinIds).stream()
                        .collect(
                                Collectors.toMap(
                                        ExchangeCoinJpaEntity::getCoinId,
                                        ExchangeCoinJpaEntity::getId));
        return new ExchangeCoinIdMap(map);
    }

    @Override
    public ExchangeCoins findByExchangeId(Long exchangeId) {
        List<ExchangeCoin> exchangeCoins =
                repository.findByExchangeId(exchangeId).stream()
                        .map(ExchangeCoinJpaEntity::toDomain)
                        .toList();
        return new ExchangeCoins(exchangeCoins);
    }
}
