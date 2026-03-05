package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.ExchangeCoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("rankingExchangeCoinQueryAdapter")
@RequiredArgsConstructor
public class ExchangeCoinQueryAdapter implements ExchangeCoinQueryPort {

    private final ExchangeCoinJpaRepository exchangeCoinJpaRepository;

    @Override
    public Optional<Long> findExchangeCoinId(Long exchangeId, Long coinId) {
        return exchangeCoinJpaRepository.findByExchangeIdAndCoinId(exchangeId, coinId)
            .map(ExchangeCoinJpaEntity::getId);
    }

    @Override
    public Map<Long, Long> findExchangeCoinIdsByExchangeIdAndCoinIds(Long exchangeId, List<Long> coinIds) {
        return exchangeCoinJpaRepository.findByExchangeIdAndCoinIdIn(exchangeId, coinIds).stream()
            .collect(Collectors.toMap(ExchangeCoinJpaEntity::getCoinId, ExchangeCoinJpaEntity::getId));
    }
}
