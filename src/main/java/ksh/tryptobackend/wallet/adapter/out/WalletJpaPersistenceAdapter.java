package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.application.port.out.WalletPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WalletJpaPersistenceAdapter implements WalletPort {

    private final WalletJpaRepository repository;
    private final WalletBalanceJpaRepository balanceRepository;

    @Override
    public Long createWallet(Long roundId, Long exchangeId, LocalDateTime createdAt) {
        Wallet wallet = Wallet.create(roundId, exchangeId, createdAt);
        WalletJpaEntity saved = repository.save(WalletJpaEntity.fromDomain(wallet));
        return saved.getId();
    }

    @Override
    public Long createWalletWithBalance(Long roundId, Long exchangeId, Long baseCurrencyCoinId,
                                        BigDecimal initialAmount, LocalDateTime createdAt) {
        Long walletId = createWallet(roundId, exchangeId, createdAt);
        balanceRepository.save(
            new WalletBalanceJpaEntity(walletId, baseCurrencyCoinId, initialAmount, BigDecimal.ZERO));
        return walletId;
    }
}
