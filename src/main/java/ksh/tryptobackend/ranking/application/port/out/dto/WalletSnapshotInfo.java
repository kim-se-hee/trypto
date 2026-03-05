package ksh.tryptobackend.ranking.application.port.out.dto;

import java.math.BigDecimal;

public record WalletSnapshotInfo(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
}
