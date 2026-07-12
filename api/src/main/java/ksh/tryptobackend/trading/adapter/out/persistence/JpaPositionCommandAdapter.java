package ksh.tryptobackend.trading.adapter.out.persistence;

import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.PositionJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.PositionJpaRepository;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPositionCommandAdapter implements PositionCommandPort {

    private final PositionJpaRepository repository;

    @Override
    public Position getOrCreate(Long walletId, Long coinId) {
        return repository
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(PositionJpaEntity::toDomain)
                .orElseGet(() -> Position.empty(walletId, coinId));
    }

    @Override
    public Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId).map(PositionJpaEntity::toDomain);
    }

    @Override
    public Position save(Position position) {
        PositionJpaEntity entity;
        if (position.getId() != null) {
            entity = repository.findById(position.getId()).orElseThrow();
        } else {
            entity = createEntity(position.getWalletId(), position.getCoinId());
        }
        entity.updateFrom(position);
        return repository.save(entity).toDomain();
    }

    private PositionJpaEntity createEntity(Long walletId, Long coinId) {
        try {
            return repository.saveAndFlush(new PositionJpaEntity(walletId, coinId));
        } catch (DataIntegrityViolationException e) {
            return repository.findByWalletIdAndCoinId(walletId, coinId).orElseThrow(() -> e);
        }
    }
}
