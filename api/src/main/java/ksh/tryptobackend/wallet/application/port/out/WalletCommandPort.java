package ksh.tryptobackend.wallet.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.domain.model.Wallet;

public interface WalletCommandPort {

    Wallet save(Wallet wallet);

    void deductBalance(Long walletId, Long coinId, BigDecimal amount);

    void addBalance(Long walletId, Long coinId, BigDecimal amount);
}
