package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcBenchmark;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.CapitalInflows;
import ksh.tryptobackend.regretanalysis.domain.vo.CumulativeLossTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.DailyAsset;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationMarkers;

public record RegretChartResult(
        Long roundId, int totalDays, List<DailyComparison> assetHistory, List<ViolationMarkerPoint> violationMarkers) {

    public static RegretChartResult empty(Long roundId) {
        return new RegretChartResult(roundId, 0, List.of(), List.of());
    }

    public static RegretChartResult from(
            Long roundId,
            AssetTimeline timeline,
            CapitalInflows capitalInflows,
            BtcDailyPrices btcDailyPrices,
            List<ViolationLoss> violationLosses) {
        CumulativeLossTimeline lossTimeline = CumulativeLossTimeline.build(violationLosses, timeline.getDates());
        BtcBenchmark btcBenchmark = BtcBenchmark.calculate(capitalInflows, btcDailyPrices, timeline.getDates());
        ViolationMarkers violationMarkers = ViolationMarkers.from(violationLosses, timeline);

        return new RegretChartResult(
                roundId,
                timeline.calculateTotalDays(),
                toAssetHistory(timeline, lossTimeline, btcBenchmark),
                toViolationMarkerPoints(violationMarkers));
    }

    private static List<DailyComparison> toAssetHistory(
            AssetTimeline timeline, CumulativeLossTimeline lossTimeline, BtcBenchmark btcBenchmark) {
        return timeline.getDailyAssets().stream()
                .map(dailyAsset -> toDailyComparison(dailyAsset, lossTimeline, btcBenchmark))
                .toList();
    }

    private static DailyComparison toDailyComparison(
            DailyAsset dailyAsset, CumulativeLossTimeline lossTimeline, BtcBenchmark btcBenchmark) {
        LocalDate date = dailyAsset.date();
        return new DailyComparison(
                date,
                dailyAsset.amount(),
                lossTimeline.calculateRuleFollowedAsset(dailyAsset.amount(), date),
                btcBenchmark.getAssetValueAt(date));
    }

    private static List<ViolationMarkerPoint> toViolationMarkerPoints(ViolationMarkers violationMarkers) {
        return violationMarkers.getMarkers().stream()
                .map(marker -> new ViolationMarkerPoint(marker.date(), marker.assetValue()))
                .toList();
    }

    public record DailyComparison(
            LocalDate snapshotDate, BigDecimal actualAsset, BigDecimal ruleFollowedAsset, BigDecimal btcHoldAsset) {}

    public record ViolationMarkerPoint(LocalDate snapshotDate, BigDecimal assetValue) {}
}
