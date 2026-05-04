package ksh.tryptobackend.wallet.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface WalletCommandPort {

    Long createWallet(
            Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt);

    Long createWalletWithBalance(
            Long roundId,
            Long exchangeId,
            Long baseCurrencyCoinId,
            BigDecimal initialAmount,
            LocalDateTime createdAt);

    void deductBalance(Long walletId, Long coinId, BigDecimal amount);

    void addBalance(Long walletId, Long coinId, BigDecimal amount);

    void lockBalance(Long walletId, Long coinId, BigDecimal amount);

    void unlockBalance(Long walletId, Long coinId, BigDecimal amount);
}
