package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetSnapshot(
    Long snapshotId,
    Long roundId,
    Long exchangeId,
    BigDecimal totalAsset,
    BigDecimal totalInvestment,
    BigDecimal totalProfitRate,
    LocalDateTime snapshotDate
) {
}
