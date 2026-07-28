package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 날짜마다 "원칙을 모두 지켰다면 얼마였을까"를 알려주는 타임라인.
 *
 * <p>아직 매도되지 않은 위반 손실은 금액이 현재가를 따라 움직이므로 그대로 더하고, 매도로 확정된 위반 손실은 실현일의 자산 대비 비율(배수)로 환산해 이후 자산에 곱한다.
 * 실현된 몫은 지갑에 섞여 이후 투자와 함께 굴러가는데, 확정 금액을 그대로 빼면 시장이 이미 가져간 금액을 한 번 더 빼는 이중 차감이 되기 때문이다. 긴급 충전은 위반과 무관한 새
 * 돈이므로 그날의 배수를 1 쪽으로 되돌린다.
 */
public final class RuleFollowedAssetTimeline {

    private static final int MULTIPLIER_SCALE = 20;
    private static final int AMOUNT_SCALE = 8;

    private final Map<LocalDate, LossAdjustment> adjustmentByDate;
    private final LossAdjustment closingAdjustment;

    private RuleFollowedAssetTimeline(Map<LocalDate, LossAdjustment> adjustmentByDate, LossAdjustment closing) {
        this.adjustmentByDate = Map.copyOf(adjustmentByDate);
        this.closingAdjustment = closing;
    }

    public static RuleFollowedAssetTimeline build(
            List<ViolationLoss> violationLosses, List<EmergencyCharge> charges, AssetTimeline timeline) {
        List<ViolationLoss> sortedLosses = sortedByOccurredDate(violationLosses);
        List<RealizedLoss> sortedRealizations = sortedByRealizedOn(violationLosses);
        List<EmergencyCharge> sortedCharges = sortedByChargedDate(charges);

        Map<LocalDate, LossAdjustment> adjustments = new LinkedHashMap<>();
        BigDecimal occurredLoss = BigDecimal.ZERO;
        BigDecimal realizedLoss = BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE;
        LossAdjustment closing = null;
        int lossIndex = 0;
        int realizationIndex = 0;
        int chargeIndex = 0;

        for (LocalDate date : timeline.getDates()) {
            while (lossIndex < sortedLosses.size()
                    && !sortedLosses.get(lossIndex).occurredDate().isAfter(date)) {
                occurredLoss =
                        occurredLoss.add(sortedLosses.get(lossIndex).krwLoss().totalAmount());
                lossIndex++;
            }

            BigDecimal realizedOnDate = BigDecimal.ZERO;
            while (realizationIndex < sortedRealizations.size()
                    && !sortedRealizations.get(realizationIndex).realizedOn().isAfter(date)) {
                realizedOnDate = realizedOnDate.add(
                        sortedRealizations.get(realizationIndex).amount());
                realizationIndex++;
            }

            BigDecimal chargedOnDate = BigDecimal.ZERO;
            while (chargeIndex < sortedCharges.size()
                    && !sortedCharges.get(chargeIndex).chargedDate().isAfter(date)) {
                chargedOnDate = chargedOnDate.add(sortedCharges.get(chargeIndex).amount());
                chargeIndex++;
            }

            realizedLoss = realizedLoss.add(realizedOnDate);
            multiplier = nextMultiplier(multiplier, assetAt(timeline, date), chargedOnDate, realizedOnDate);
            closing = new LossAdjustment(occurredLoss.subtract(realizedLoss), multiplier);
            adjustments.put(date, closing);
        }

        return new RuleFollowedAssetTimeline(adjustments, closing != null ? closing : additiveOnly(sortedLosses));
    }

    /**
     * 실현도 충전도 없는 날은 값이 그대로다. 실현이 있으면 그 몫만큼 배수가 움직이고, 충전이 있으면 새 돈까지 위반 몫으로 깎이지 않도록 되돌린다. 자산은 음수가 될 수
     * 없으므로 배수의 하한은 0 이다.
     */
    private static BigDecimal nextMultiplier(
            BigDecimal multiplier, BigDecimal totalAsset, BigDecimal charged, BigDecimal realized) {
        if (totalAsset.signum() <= 0 || (charged.signum() == 0 && realized.signum() == 0)) {
            return multiplier;
        }

        BigDecimal adjusted =
                multiplier.multiply(totalAsset.subtract(charged).add(realized)).add(charged);
        return adjusted.divide(totalAsset, MULTIPLIER_SCALE, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }

    private static BigDecimal assetAt(AssetTimeline timeline, LocalDate date) {
        return timeline.findAssetAt(date).orElse(BigDecimal.ZERO);
    }

    /** 일별 자산이 하나도 없으면 배수를 만들 근거가 없다. 이때는 위반 손실을 금액 그대로 얹는 것이 최선의 근사다. */
    private static LossAdjustment additiveOnly(List<ViolationLoss> losses) {
        BigDecimal totalLoss =
                losses.stream().map(loss -> loss.krwLoss().totalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LossAdjustment(totalLoss, BigDecimal.ONE);
    }

    private static List<ViolationLoss> sortedByOccurredDate(List<ViolationLoss> losses) {
        return losses.stream()
                .sorted(Comparator.comparing(ViolationLoss::occurredDate))
                .toList();
    }

    private static List<RealizedLoss> sortedByRealizedOn(List<ViolationLoss> losses) {
        List<RealizedLoss> realizations = new ArrayList<>();
        losses.forEach(loss -> realizations.addAll(loss.krwLoss().realizedLosses()));
        return realizations.stream()
                .sorted(Comparator.comparing(RealizedLoss::realizedOn))
                .toList();
    }

    private static List<EmergencyCharge> sortedByChargedDate(List<EmergencyCharge> charges) {
        return charges.stream()
                .sorted(Comparator.comparing(EmergencyCharge::chargedDate))
                .toList();
    }

    public BigDecimal calculateRuleFollowedAsset(BigDecimal actualAsset, LocalDate date) {
        LossAdjustment adjustment = adjustmentByDate.get(date);
        if (adjustment == null) {
            return actualAsset;
        }
        return adjustment.applyTo(actualAsset);
    }

    /** 라운드 요약이 쓰는 마지막 날 기준 값. 곡선의 끝 점과 같은 계산이다. */
    public BigDecimal calculateFinalAsset(BigDecimal actualAsset) {
        return closingAdjustment.applyTo(actualAsset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleFollowedAssetTimeline that)) return false;
        return Objects.equals(adjustmentByDate, that.adjustmentByDate)
                && Objects.equals(closingAdjustment, that.closingAdjustment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adjustmentByDate, closingAdjustment);
    }

    /** 그날까지의 미실현 위반 손실 합과 누적 배수. 실제 자산에 앞엣것을 더한 뒤 뒤엣것을 곱하면 원칙 준수 시 자산이 된다. */
    public record LossAdjustment(BigDecimal unrealizedLoss, BigDecimal multiplier) {

        public BigDecimal applyTo(BigDecimal actualAsset) {
            return actualAsset.add(unrealizedLoss).multiply(multiplier).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
    }
}
