package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.ExchangeCoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("rankingExchangeCoinQueryAdapter")
@RequiredArgsConstructor
public class ExchangeCoinQueryAdapter implements ExchangeCoinQueryPort {

    private final ExchangeCoinJpaRepository exchangeCoinJpaRepository;

    @Override
    public Optional<Long> findExchangeCoinId(Long exchangeId, Long coinId) {
        return exchangeCoinJpaRepository.findByExchangeIdAndCoinId(exchangeId, coinId)
            .map(entity -> entity.getId());
    }
}
