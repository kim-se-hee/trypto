package ksh.tryptobackend.trading.adapter.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.entity.HoldingJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.HoldingJpaRepository;
import ksh.tryptobackend.trading.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldingQueryAdapter implements HoldingQueryPort {

    private final HoldingJpaRepository repository;

    @Override
    public List<Holding> findAllByWalletId(Long walletId) {
        return repository.findByWalletId(walletId).stream()
                .map(HoldingJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId).map(HoldingJpaEntity::toDomain);
    }
}
