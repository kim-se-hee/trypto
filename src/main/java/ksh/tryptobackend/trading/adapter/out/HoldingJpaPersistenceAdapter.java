package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.adapter.out.entity.HoldingJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.HoldingJpaRepository;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HoldingJpaPersistenceAdapter implements HoldingPersistencePort {

    private final HoldingJpaRepository repository;

    @Override
    public Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .map(HoldingJpaEntity::toDomain);
    }

    @Override
    public List<Holding> findAllByWalletId(Long walletId) {
        return repository.findByWalletId(walletId).stream()
            .map(HoldingJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Holding save(Holding holding) {
        HoldingJpaEntity entity = repository.findByWalletIdAndCoinId(
                holding.getWalletId(), holding.getCoinId())
            .orElseGet(() -> new HoldingJpaEntity(holding.getWalletId(), holding.getCoinId()));
        entity.updateFrom(holding);
        return repository.save(entity).toDomain();
    }
}
