package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinChainJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeCoinChainInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeCoinChainJpaPersistenceAdapter implements ExchangeCoinChainQueryPort {

    private final ExchangeCoinChainJpaRepository repository;

    @Override
    public Optional<ExchangeCoinChainInfo> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain) {
        return repository.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .map(entity -> new ExchangeCoinChainInfo(
                entity.getId(), entity.getExchangeCoinId(), entity.getChain(), entity.isTagRequired()));
    }
}
