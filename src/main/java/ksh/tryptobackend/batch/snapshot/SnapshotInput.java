package ksh.tryptobackend.batch.snapshot;

import java.math.BigDecimal;

public record SnapshotInput(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    BigDecimal seedAmount
) {
}
