package ksh.tryptobackend.wallet.application.port.out.dto;

import java.math.BigDecimal;

public record WalletInfo(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
}
