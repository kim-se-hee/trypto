package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcBenchmark;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.CapitalInflows;
import ksh.tryptobackend.regretanalysis.domain.vo.DailyAsset;
import ksh.tryptobackend.regretanalysis.domain.vo.EmergencyCharge;
import ksh.tryptobackend.regretanalysis.domain.vo.RuleFollowedAssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationMarkers;

public record RegretChartResult(
        Long roundId,
        int totalDays,
        List<DailyComparison> assetHistory,
        List<ViolationMarkerPoint> violationMarkers,
        List<EmergencyChargePoint> emergencyCharges) {

    public static RegretChartResult empty(Long roundId) {
        return new RegretChartResult(roundId, 0, List.of(), List.of(), List.of());
    }

    public static RegretChartResult from(
            Long roundId,
            AssetTimeline timeline,
            CapitalInflows capitalInflows,
            List<EmergencyCharge> emergencyCharges,
            BtcDailyPrices btcDailyPrices,
            List<ViolationLoss> violationLosses) {
        RuleFollowedAssetTimeline ruleFollowedAssets =
                RuleFollowedAssetTimeline.build(violationLosses, emergencyCharges, timeline);
        BtcBenchmark btcBenchmark = BtcBenchmark.calculate(capitalInflows, btcDailyPrices, timeline.getDates());
        ViolationMarkers violationMarkers = ViolationMarkers.from(violationLosses, timeline);

        return new RegretChartResult(
                roundId,
                timeline.calculateTotalDays(),
                toAssetHistory(timeline, ruleFollowedAssets, btcBenchmark),
                toViolationMarkerPoints(violationMarkers),
                toEmergencyChargePoints(emergencyCharges));
    }

    private static List<DailyComparison> toAssetHistory(
            AssetTimeline timeline, RuleFollowedAssetTimeline ruleFollowedAssets, BtcBenchmark btcBenchmark) {
        return timeline.getDailyAssets().stream()
                .map(dailyAsset -> toDailyComparison(dailyAsset, ruleFollowedAssets, btcBenchmark))
                .toList();
    }

    private static DailyComparison toDailyComparison(
            DailyAsset dailyAsset, RuleFollowedAssetTimeline ruleFollowedAssets, BtcBenchmark btcBenchmark) {
        LocalDate date = dailyAsset.date();
        return new DailyComparison(
                date,
                dailyAsset.amount(),
                ruleFollowedAssets.calculateRuleFollowedAsset(dailyAsset.amount(), date),
                btcBenchmark.getAssetValueAt(date));
    }

    private static List<ViolationMarkerPoint> toViolationMarkerPoints(ViolationMarkers violationMarkers) {
        return violationMarkers.getMarkers().stream()
                .map(marker -> new ViolationMarkerPoint(marker.date(), marker.assetValue()))
                .toList();
    }

    private static List<EmergencyChargePoint> toEmergencyChargePoints(List<EmergencyCharge> emergencyCharges) {
        return emergencyCharges.stream()
                .map(charge -> new EmergencyChargePoint(charge.chargedDate(), charge.amount()))
                .toList();
    }

    public record DailyComparison(
            LocalDate snapshotDate, BigDecimal actualAsset, BigDecimal ruleFollowedAsset, BigDecimal btcHoldAsset) {}

    public record ViolationMarkerPoint(LocalDate snapshotDate, BigDecimal assetValue) {}

    public record EmergencyChargePoint(LocalDate chargedDate, BigDecimal amount) {}
}
