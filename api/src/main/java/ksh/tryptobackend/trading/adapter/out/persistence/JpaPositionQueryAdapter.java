package ksh.tryptobackend.trading.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.PositionJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.PositionJpaRepository;
import ksh.tryptobackend.trading.application.port.out.PositionQueryPort;
import ksh.tryptobackend.trading.domain.model.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPositionQueryAdapter implements PositionQueryPort {

    private final PositionJpaRepository repository;

    @Override
    public List<Position> findAllByWalletId(Long walletId) {
        return repository.findByWalletId(walletId).stream()
                .map(PositionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(PositionJpaEntity::toDomain);
    }
}
