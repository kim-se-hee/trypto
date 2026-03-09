package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;

public record SnapshotInputResult(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    BigDecimal seedAmount
) {
}
