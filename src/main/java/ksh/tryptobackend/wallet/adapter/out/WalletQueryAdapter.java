package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletQueryAdapter implements WalletQueryPort {

    private final WalletJpaRepository walletRepository;
    private final WalletBalanceJpaRepository balanceRepository;

    @Override
    public Optional<WalletInfo> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return walletRepository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(this::toWalletInfo);
    }

    @Override
    public Optional<WalletInfo> findById(Long walletId) {
        return walletRepository.findById(walletId)
            .map(this::toWalletInfo);
    }

    @Override
    public List<WalletInfo> findByRoundId(Long roundId) {
        return walletRepository.findByRoundId(roundId).stream()
            .map(this::toWalletInfo)
            .toList();
    }

    @Override
    public List<WalletInfo> findByRoundIds(List<Long> roundIds) {
        if (roundIds.isEmpty()) {
            return List.of();
        }
        return walletRepository.findByRoundIdIn(roundIds).stream()
            .map(this::toWalletInfo)
            .toList();
    }

    @Override
    public List<WalletInfo> findByExchangeId(Long exchangeId) {
        return walletRepository.findByExchangeId(exchangeId).stream()
            .map(this::toWalletInfo)
            .toList();
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return balanceRepository.findByWalletIdAndCoinId(walletId, coinId)
            .map(WalletBalanceJpaEntity::getAvailable)
            .orElse(BigDecimal.ZERO);
    }

    private WalletInfo toWalletInfo(WalletJpaEntity wallet) {
        return new WalletInfo(wallet.getId(), wallet.getRoundId(), wallet.getExchangeId(),
            wallet.getSeedAmount());
    }
}
