package ksh.tryptobackend.trading.adapter.out.persistence;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.PositionJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.PositionJpaRepository;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.TransferPositions;
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
    public TransferPositions getTransferPositionsWithLock(Long coinId, Long fromWalletId, Long toWalletId) {
        Map<Long, Position> lockedByWalletId = new LinkedHashMap<>();
        Stream.of(fromWalletId, toWalletId)
                .sorted()
                .forEach(walletId -> lockedByWalletId.put(walletId, getOrCreateWithLock(walletId, coinId)));
        return new TransferPositions(lockedByWalletId.get(fromWalletId), lockedByWalletId.get(toWalletId));
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

    @Override
    public void saveAll(List<Position> positions) {
        positions.stream()
                .sorted(Comparator.comparing(Position::getWalletId).thenComparing(Position::getCoinId))
                .forEach(this::save);
    }

    private Position getOrCreateWithLock(Long walletId, Long coinId) {
        return repository
                .findWithLockByWalletIdAndCoinId(walletId, coinId)
                .map(PositionJpaEntity::toDomain)
                .orElseGet(() -> Position.empty(walletId, coinId));
    }

    private PositionJpaEntity createEntity(Long walletId, Long coinId) {
        try {
            return repository.saveAndFlush(new PositionJpaEntity(walletId, coinId));
        } catch (DataIntegrityViolationException e) {
            return repository.findByWalletIdAndCoinId(walletId, coinId).orElseThrow(() -> e);
        }
    }
}
