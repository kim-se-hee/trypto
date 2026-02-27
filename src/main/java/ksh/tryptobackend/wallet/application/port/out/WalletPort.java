package ksh.tryptobackend.wallet.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface WalletPort {

    Long createWallet(Long roundId, Long exchangeId, LocalDateTime createdAt);

    Long createWalletWithBalance(Long roundId, Long exchangeId, Long baseCurrencyCoinId,
                                 BigDecimal initialAmount, LocalDateTime createdAt);
}
