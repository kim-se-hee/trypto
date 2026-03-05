package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletJpaPersistenceAdapter implements WalletPort, WalletQueryPort {

    private final WalletJpaRepository repository;
    private final WalletBalanceJpaRepository balanceRepository;

    @Override
    public Long createWallet(Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt) {
        Wallet wallet = Wallet.create(roundId, exchangeId, seedAmount, createdAt);
        WalletJpaEntity saved = repository.save(WalletJpaEntity.fromDomain(wallet));
        return saved.getId();
    }

    @Override
    public Long createWalletWithBalance(Long roundId, Long exchangeId, Long baseCurrencyCoinId,
                                        BigDecimal initialAmount, LocalDateTime createdAt) {
        Long walletId = createWallet(roundId, exchangeId, initialAmount, createdAt);
        balanceRepository.save(
            new WalletBalanceJpaEntity(walletId, baseCurrencyCoinId, initialAmount, BigDecimal.ZERO));
        return walletId;
    }

    @Override
    public Optional<WalletInfo> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(this::toWalletInfo);
    }

    @Override
    public Optional<WalletInfo> findById(Long walletId) {
        return repository.findById(walletId)
            .map(this::toWalletInfo);
    }

    @Override
    public List<WalletInfo> findByRoundId(Long roundId) {
        return repository.findByRoundId(roundId).stream()
            .map(this::toWalletInfo)
            .toList();
    }

    @Override
    public List<WalletInfo> findByRoundIds(List<Long> roundIds) {
        if (roundIds.isEmpty()) {
            return List.of();
        }
        return repository.findByRoundIdIn(roundIds).stream()
            .map(this::toWalletInfo)
            .toList();
    }

    private WalletInfo toWalletInfo(WalletJpaEntity wallet) {
        return new WalletInfo(wallet.getId(), wallet.getRoundId(), wallet.getExchangeId(),
            wallet.getSeedAmount());
    }
}
