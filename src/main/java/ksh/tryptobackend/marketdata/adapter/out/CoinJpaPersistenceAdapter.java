package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CoinJpaPersistenceAdapter implements CoinQueryPort {

    private final CoinJpaRepository coinJpaRepository;
    private final ExchangeCoinJpaRepository exchangeCoinJpaRepository;

    @Override
    public Map<Long, String> findSymbolsByIds(Set<Long> coinIds) {
        if (coinIds.isEmpty()) {
            return Map.of();
        }
        return coinJpaRepository.findByIdIn(coinIds).stream()
            .collect(Collectors.toMap(CoinJpaEntity::getId, CoinJpaEntity::getSymbol));
    }

    @Override
    public Map<Long, String> findSymbolsByExchangeCoinIds(Set<Long> exchangeCoinIds) {
        if (exchangeCoinIds.isEmpty()) {
            return Map.of();
        }

        List<ExchangeCoinJpaEntity> exchangeCoins = exchangeCoinJpaRepository.findByIdIn(exchangeCoinIds);

        Set<Long> coinIds = exchangeCoins.stream()
            .map(ExchangeCoinJpaEntity::getCoinId)
            .collect(Collectors.toSet());

        Map<Long, String> coinSymbolMap = findSymbolsByIds(coinIds);

        return exchangeCoins.stream()
            .collect(Collectors.toMap(
                ExchangeCoinJpaEntity::getId,
                ec -> coinSymbolMap.getOrDefault(ec.getCoinId(), "")
            ));
    }

    @Override
    public Map<Long, Long> findCoinIdsByExchangeCoinIds(Set<Long> exchangeCoinIds) {
        if (exchangeCoinIds.isEmpty()) {
            return Map.of();
        }
        return exchangeCoinJpaRepository.findByIdIn(exchangeCoinIds).stream()
            .collect(Collectors.toMap(ExchangeCoinJpaEntity::getId, ExchangeCoinJpaEntity::getCoinId));
    }
}
