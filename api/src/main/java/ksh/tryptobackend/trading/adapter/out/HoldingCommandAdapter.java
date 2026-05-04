package ksh.tryptobackend.trading.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.entity.HoldingJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.HoldingJpaRepository;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldingCommandAdapter implements HoldingCommandPort {

    private final HoldingJpaRepository repository;

    @Override
    public Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId).map(HoldingJpaEntity::toDomain);
    }

    @Override
    public Holding save(Holding holding) {
        HoldingJpaEntity entity;
        if (holding.getId() != null) {
            entity = repository.findById(holding.getId()).orElseThrow();
        } else {
            entity = createHoldingEntity(holding.getWalletId(), holding.getCoinId());
        }
        entity.updateFrom(holding);
        return repository.save(entity).toDomain();
    }

    private HoldingJpaEntity createHoldingEntity(Long walletId, Long coinId) {
        try {
            return repository.saveAndFlush(new HoldingJpaEntity(walletId, coinId));
        } catch (DataIntegrityViolationException e) {
            return repository.findByWalletIdAndCoinId(walletId, coinId).orElseThrow(() -> e);
        }
    }
}
