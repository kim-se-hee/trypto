package ksh.tryptobackend.ranking.application.port.out;

import java.math.BigDecimal;

public interface BalanceQueryPort {

    BigDecimal getAvailableBalance(Long walletId, Long coinId);
}
