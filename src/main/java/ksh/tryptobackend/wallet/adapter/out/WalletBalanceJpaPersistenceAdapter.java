package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class WalletBalanceJpaPersistenceAdapter implements WalletBalancePort {

    private final WalletBalanceJpaRepository repository;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .map(WalletBalanceJpaEntity::getAvailable)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntity(walletId, coinId);
        entity.deductAvailable(amount);
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntity(walletId, coinId);
        entity.addAvailable(amount);
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntity(walletId, coinId);
        entity.lock(amount);
    }

    @Override
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntity(walletId, coinId);
        entity.unlock(amount);
    }

    private WalletBalanceJpaEntity getOrCreateEntity(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> repository.save(
                new WalletBalanceJpaEntity(walletId, coinId, BigDecimal.ZERO, BigDecimal.ZERO)));
    }
}
