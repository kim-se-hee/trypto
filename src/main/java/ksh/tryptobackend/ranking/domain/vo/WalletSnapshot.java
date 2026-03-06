package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record WalletSnapshot(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
}
