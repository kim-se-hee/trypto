package ksh.tryptobackend.wallet.application.port.in;

import java.math.BigDecimal;

public interface ManageWalletBalanceUseCase {

    void deductBalance(Long walletId, Long coinId, BigDecimal amount);

    void addBalance(Long walletId, Long coinId, BigDecimal amount);

    void lockBalance(Long walletId, Long coinId, BigDecimal amount);

    void unlockBalance(Long walletId, Long coinId, BigDecimal amount);
}
