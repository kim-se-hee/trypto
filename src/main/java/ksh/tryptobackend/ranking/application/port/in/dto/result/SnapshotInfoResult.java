package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SnapshotInfoResult(
    Long snapshotId,
    Long roundId,
    Long exchangeId,
    BigDecimal totalAsset,
    BigDecimal totalInvestment,
    BigDecimal totalProfitRate,
    LocalDate snapshotDate
) {
}
