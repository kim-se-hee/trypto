package ksh.tryptobackend.batch.regretreport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegretReportInput(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    LocalDateTime startedAt
) {
}
