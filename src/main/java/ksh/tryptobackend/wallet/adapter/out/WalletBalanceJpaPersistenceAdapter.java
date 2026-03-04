package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceOperationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class WalletBalanceJpaPersistenceAdapter implements WalletBalanceOperationPort {

    private final WalletBalanceJpaRepository repository;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return repository.findByWalletIdAndCoinId(walletId, coinId)
            .map(WalletBalanceJpaEntity::getAvailable)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntityForUpdate(walletId, coinId);
        entity.deductAvailable(amount);
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntityForUpdate(walletId, coinId);
        entity.addAvailable(amount);
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntityForUpdate(walletId, coinId);
        entity.lock(amount);
    }

    @Override
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        WalletBalanceJpaEntity entity = getOrCreateEntityForUpdate(walletId, coinId);
        entity.unlock(amount);
    }

    private WalletBalanceJpaEntity getOrCreateEntityForUpdate(Long walletId, Long coinId) {
        return repository.findForUpdateByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> createEntity(walletId, coinId));
    }

    private WalletBalanceJpaEntity createEntity(Long walletId, Long coinId) {
        try {
            return repository.saveAndFlush(
                new WalletBalanceJpaEntity(walletId, coinId, BigDecimal.ZERO, BigDecimal.ZERO));
        } catch (DataIntegrityViolationException e) {
            return repository.findForUpdateByWalletIdAndCoinId(walletId, coinId)
                .orElseThrow(() -> e);
        }
    }
}
