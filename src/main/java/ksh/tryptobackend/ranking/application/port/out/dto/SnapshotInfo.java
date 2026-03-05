package ksh.tryptobackend.ranking.application.port.out.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SnapshotInfo(
    Long snapshotId,
    Long roundId,
    Long exchangeId,
    BigDecimal totalAsset,
    BigDecimal totalInvestment,
    BigDecimal totalProfitRate,
    LocalDate snapshotDate
) {
}
