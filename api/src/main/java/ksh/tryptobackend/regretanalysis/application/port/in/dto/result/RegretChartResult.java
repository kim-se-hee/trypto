package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcBenchmark;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.CumulativeLossTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationMarkers;

public record RegretChartResult(
        Long roundId,
        Long exchangeId,
        String exchangeName,
        String currency,
        int totalDays,
        List<DailyComparison> assetHistory,
        List<ViolationMarkerPoint> violationMarkers) {

    public static RegretChartResult empty(Long roundId, AnalysisExchange exchange) {
        return new RegretChartResult(
                roundId, exchange.exchangeId(), exchange.name(), exchange.currency(), 0, List.of(), List.of());
    }

    public static RegretChartResult from(
            Long roundId,
            AnalysisExchange exchange,
            AssetTimeline timeline,
            BtcDailyPrices btcDailyPrices,
            List<ViolationDetail> violations) {
        CumulativeLossTimeline lossTimeline = CumulativeLossTimeline.build(violations, timeline.getDates());
        BtcBenchmark btcBenchmark = BtcBenchmark.calculate(
                timeline.getSeedMoney(), btcDailyPrices.toMap(), timeline.getDates(), timeline.getStartDate());
        ViolationMarkers violationMarkers = ViolationMarkers.from(violations, timeline);

        return new RegretChartResult(
                roundId,
                exchange.exchangeId(),
                exchange.name(),
                exchange.currency(),
                timeline.calculateTotalDays(),
                toAssetHistory(timeline, lossTimeline, btcBenchmark),
                toViolationMarkerPoints(violationMarkers));
    }

    private static List<DailyComparison> toAssetHistory(
            AssetTimeline timeline, CumulativeLossTimeline lossTimeline, BtcBenchmark btcBenchmark) {
        return timeline.getSnapshots().stream()
                .map(snapshot -> {
                    LocalDate date = snapshot.getSnapshotDate();
                    BigDecimal actualAsset = snapshot.getTotalAsset();
                    return new DailyComparison(
                            date,
                            actualAsset,
                            lossTimeline.calculateRuleFollowedAsset(actualAsset, date),
                            btcBenchmark.getAssetValueAt(date));
                })
                .toList();
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
