package ksh.tryptobackend.portfolio.application.port.out.dto;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;

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

    public SnapshotInfoResult toResult() {
        return new SnapshotInfoResult(
            snapshotId, roundId, exchangeId,
            totalAsset, totalInvestment, totalProfitRate, snapshotDate
        );
    }
}
