package ksh.tryptobackend.wallet.adapter.out;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.adapter.out.persistence.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.persistence.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.domain.model.TransferBalances;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletBalanceQueryAdapter implements WalletBalanceQueryPort {

    private final WalletBalanceJpaRepository walletBalanceRepository;

    @Override
    public List<WalletBalance> findByWalletId(Long walletId) {
        return walletBalanceRepository.findByWalletId(walletId).stream()
                .map(WalletBalanceJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<WalletBalance> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return walletBalanceRepository.findByWalletIdAndCoinId(walletId, coinId).map(WalletBalanceJpaEntity::toDomain);
    }

    @Override
    public List<WalletBalance> getAllByWalletIdAndCoinIdsWithLock(Long walletId, List<Long> coinIds) {
        return coinIds.stream()
                .sorted()
                .map(coinId -> getWithLock(walletId, coinId))
                .toList();
    }

    @Override
    public TransferBalances getTransferBalancesWithLock(Long coinId, Long fromWalletId, Long toWalletId) {
        Map<Long, WalletBalance> lockedByWalletId = new LinkedHashMap<>();
        Stream.of(fromWalletId, toWalletId)
                .sorted()
                .forEach(walletId -> lockedByWalletId.put(walletId, getWithLock(walletId, coinId)));
        return new TransferBalances(lockedByWalletId.get(fromWalletId), lockedByWalletId.get(toWalletId));
    }

    private WalletBalance getWithLock(Long walletId, Long coinId) {
        return walletBalanceRepository
                .findWithLockByWalletIdAndCoinId(walletId, coinId)
                .map(WalletBalanceJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_BALANCE_NOT_FOUND));
    }
}
