package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.math.BigDecimal;

public record WalletResult(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
}
