package ksh.tryptobackend.wallet.adapter.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletQueryAdapter implements WalletQueryPort {

    private final WalletJpaRepository walletRepository;

    @Override
    public Optional<Wallet> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return walletRepository
                .findByRoundIdAndExchangeId(roundId, exchangeId)
                .map(WalletJpaEntity::toDomain);
    }

    @Override
    public Optional<Wallet> findById(Long walletId) {
        return walletRepository.findById(walletId).map(WalletJpaEntity::toDomain);
    }

    @Override
    public List<Wallet> findByRoundId(Long roundId) {
        return walletRepository.findByRoundId(roundId).stream()
                .map(WalletJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Wallet> findByRoundIds(List<Long> roundIds) {
        if (roundIds.isEmpty()) {
            return List.of();
        }
        return walletRepository.findByRoundIdIn(roundIds).stream()
                .map(WalletJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Wallet> findByExchangeId(Long exchangeId) {
        return walletRepository.findByExchangeId(exchangeId).stream()
                .map(WalletJpaEntity::toDomain)
                .toList();
    }
}
