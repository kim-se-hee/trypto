package ksh.tryptobackend.wallet.application.port.in;

import java.math.BigDecimal;

public interface GetAvailableBalanceUseCase {

    BigDecimal getAvailableBalance(Long walletId, Long coinId);
}
