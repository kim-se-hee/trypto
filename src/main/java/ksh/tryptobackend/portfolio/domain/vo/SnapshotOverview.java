package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SnapshotOverview(
    Long snapshotId,
    Long roundId,
    Long exchangeId,
    BigDecimal totalAsset,
    BigDecimal totalInvestment,
    BigDecimal totalProfitRate,
    LocalDate snapshotDate
) {
}
