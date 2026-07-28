package ksh.tryptobackend.regretanalysis.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;

public record RegretChartResponse(
        Long roundId,
        int totalDays,
        List<AssetHistoryItem> assetHistory,
        List<ViolationMarkerItem> violationMarkers,
        List<EmergencyChargeItem> emergencyCharges) {

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

    /** 원칙 골라 보기가 배수를 다시 굴리려면 충전일과 금액이 필요하다. 충전은 위반과 무관한 새 돈이라 그날의 배수를 1 쪽으로 되돌린다. */
    public record EmergencyChargeItem(LocalDate chargedDate, BigDecimal amount) {

        public static EmergencyChargeItem from(RegretChartResult.EmergencyChargePoint result) {
            return new EmergencyChargeItem(result.chargedDate(), result.amount());
        }
    }

    public static RegretChartResponse from(RegretChartResult result) {
        return new RegretChartResponse(
                result.roundId(),
                result.totalDays(),
                result.assetHistory().stream().map(AssetHistoryItem::from).toList(),
                result.violationMarkers().stream()
                        .map(ViolationMarkerItem::from)
                        .toList(),
                result.emergencyCharges().stream()
                        .map(EmergencyChargeItem::from)
                        .toList());
    }
}
