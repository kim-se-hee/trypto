package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegretChartResult(
        Long roundId,
        Long exchangeId,
        String exchangeName,
        String currency,
        int totalDays,
        List<DailyComparison> assetHistory,
        List<ViolationMarkerPoint> violationMarkers) {

    public record DailyComparison(
            LocalDate snapshotDate,
            BigDecimal actualAsset,
            BigDecimal ruleFollowedAsset,
            BigDecimal btcHoldAsset) {}

    public record ViolationMarkerPoint(LocalDate snapshotDate, BigDecimal assetValue) {}
}
