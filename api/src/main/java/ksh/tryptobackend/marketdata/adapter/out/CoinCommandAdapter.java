package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.out.persistence.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.application.port.out.CoinCommandPort;
import ksh.tryptobackend.marketdata.domain.model.Coin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoinCommandAdapter implements CoinCommandPort {

    private final CoinJpaRepository repository;

    @Override
    public Coin save(String symbol, String name) {
        CoinJpaEntity entity = repository
                .findBySymbol(symbol)
                .map(existing -> {
                    existing.updateName(name);
                    return existing;
                })
                .orElseGet(() -> new CoinJpaEntity(symbol, name));
        return repository.save(entity).toDomain();
    }
}
