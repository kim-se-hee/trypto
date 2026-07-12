package ksh.tryptobackend.regretanalysis.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;

public record RegretChartResponse(
        Long roundId,
        Long exchangeId,
        String exchangeName,
        String currency,
        int totalDays,
        List<AssetHistoryItem> assetHistory,
        List<ViolationMarkerItem> violationMarkers) {

    public record AssetHistoryItem(
            LocalDate snapshotDate, BigDecimal actualAsset, BigDecimal ruleFollowedAsset, BigDecimal btcHoldAsset) {

        public static AssetHistoryItem from(RegretChartResult.DailyComparison result) {
            return new AssetHistoryItem(
                    result.snapshotDate(), result.actualAsset(), result.ruleFollowedAsset(), result.btcHoldAsset());
        }
    }

    public record ViolationMarkerItem(LocalDate snapshotDate, BigDecimal assetValue) {

        public static ViolationMarkerItem from(RegretChartResult.ViolationMarkerPoint result) {
            return new ViolationMarkerItem(result.snapshotDate(), result.assetValue());
        }
    }

    public static RegretChartResponse from(RegretChartResult result) {
        return new RegretChartResponse(
                result.roundId(),
                result.exchangeId(),
                result.exchangeName(),
                result.currency(),
                result.totalDays(),
                result.assetHistory().stream().map(AssetHistoryItem::from).toList(),
                result.violationMarkers().stream()
                        .map(ViolationMarkerItem::from)
                        .toList());
    }
}
