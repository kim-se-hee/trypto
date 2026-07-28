package ksh.tryptobackend.regretanalysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RuleFollowedAssetTimelineTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);
    private static final LocalDate DAY_4 = LocalDate.of(2025, 1, 4);

    private ViolationLoss unrealizedOn(LocalDate date, String amount) {
        return new ViolationLoss(date, ViolationLossBreakdown.unrealized(new BigDecimal(amount)));
    }

    private ViolationLoss realizedOn(LocalDate occurredDate, LocalDate realizedDate, String amount) {
        return new ViolationLoss(
                occurredDate,
                new ViolationLossBreakdown(
                        BigDecimal.ZERO, List.of(new RealizedLoss(realizedDate, new BigDecimal(amount)))));
    }

    private AssetTimeline assets(Object... datesAndAmounts) {
        List<DailyAsset> dailyAssets = new java.util.ArrayList<>();
        for (int i = 0; i < datesAndAmounts.length; i += 2) {
            dailyAssets.add(
                    new DailyAsset((LocalDate) datesAndAmounts[i], new BigDecimal((String) datesAndAmounts[i + 1])));
        }
        return AssetTimeline.of(dailyAssets);
    }

    @Nested
    @DisplayName("미실현 위반 손실 가산")
    class UnrealizedLossTest {

        @Test
        @DisplayName("위반이 없으면 원칙 준수 시 자산은 실제 자산과 같다")
        void build_noViolations_equalsActualAsset() {
            RuleFollowedAssetTimeline timeline =
                    RuleFollowedAssetTimeline.build(List.of(), List.of(), assets(DAY_1, "1000000", DAY_2, "1000000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1000000"), DAY_1))
                    .isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1000000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("미실현 위반 손실은 발생일부터 이후 날짜까지 얹혀 간다")
        void build_unrealizedViolation_carriedForward() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_2, "10000")),
                    List.of(),
                    assets(DAY_1, "900000", DAY_2, "900000", DAY_3, "900000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("900000"), DAY_1))
                    .isEqualByComparingTo(new BigDecimal("900000"));
            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("900000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("910000"));
            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("900000"), DAY_3))
                    .isEqualByComparingTo(new BigDecimal("910000"));
        }

        @Test
        @DisplayName("여러 위반이 날짜순으로 누적된다")
        void build_multipleViolations_cumulativeSum() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_1, "5000"), unrealizedOn(DAY_3, "15000")),
                    List.of(),
                    assets(DAY_1, "100000", DAY_2, "100000", DAY_3, "100000", DAY_4, "100000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("100000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("105000"));
            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("100000"), DAY_4))
                    .isEqualByComparingTo(new BigDecimal("120000"));
        }

        @Test
        @DisplayName("곡선이 시작하는 날보다 앞서 일어난 위반도 첫날에 얹는다")
        void build_violationBeforeFirstDate_appliedOnFirstDate() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_1, "50000")), List.of(), assets(DAY_3, "1000000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1000000"), DAY_3))
                    .isEqualByComparingTo(new BigDecimal("1050000"));
        }
    }

    @Nested
    @DisplayName("실현 위반 손실의 배수 환산")
    class RealizedLossTest {

        @Test
        @DisplayName("실현일에는 가산과 곱셈의 값이 같아 곡선이 끊기지 않는다")
        void build_onRealizedDate_sameAsAdditive() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(realizedOn(DAY_1, DAY_2, "-700000")),
                    List.of(),
                    assets(DAY_1, "1000000", DAY_2, "1700000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1700000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("실현 이후에는 확정 금액이 아니라 그날 자산의 비율로 반영된다")
        void build_afterRealization_scalesWithPortfolio() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(realizedOn(DAY_1, DAY_1, "-700000")), List.of(), assets(DAY_1, "1700000", DAY_2, "170000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("170000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("실현 전까지는 그 몫도 금액으로 얹혀 있다")
        void build_beforeRealization_addedAsAmount() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(realizedOn(DAY_1, DAY_3, "30000")),
                    List.of(),
                    assets(DAY_1, "1000000", DAY_2, "1000000", DAY_3, "1000000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1000000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("1030000"));
        }

        @Test
        @DisplayName("배수의 하한은 0 이라 준수 시 자산이 음수가 되지 않는다")
        void build_extremeRealization_flooredAtZero() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(realizedOn(DAY_1, DAY_1, "-2000000")),
                    List.of(),
                    assets(DAY_1, "1000000", DAY_2, "1000000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1000000"), DAY_2))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("긴급 충전")
    class EmergencyChargeTest {

        @Test
        @DisplayName("충전한 돈은 위반 몫으로 깎이지 않는다")
        void build_emergencyCharge_pullsMultiplierTowardOne() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(realizedOn(DAY_1, DAY_1, "-1000000")),
                    List.of(new EmergencyCharge(DAY_2, new BigDecimal("2000000"))),
                    assets(DAY_1, "2000000", DAY_2, "4000000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("4000000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("3000000"));
        }

        @Test
        @DisplayName("실현이 없으면 충전만으로 배수가 바뀌지 않는다")
        void build_chargeWithoutRealization_keepsMultiplier() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_1, "100000")),
                    List.of(new EmergencyCharge(DAY_2, new BigDecimal("500000"))),
                    assets(DAY_1, "1000000", DAY_2, "1500000"));

            assertThat(timeline.calculateRuleFollowedAsset(new BigDecimal("1500000"), DAY_2))
                    .isEqualByComparingTo(new BigDecimal("1600000"));
        }
    }

    @Nested
    @DisplayName("라운드 요약용 마지막 날 자산")
    class FinalAssetTest {

        @Test
        @DisplayName("마지막 날의 곡선 값과 같은 계산이다")
        void calculateFinalAsset_sameAsLastCurvePoint() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_1, "50000")), List.of(), assets(DAY_1, "900000", DAY_2, "950000"));

            assertThat(timeline.calculateFinalAsset(new BigDecimal("950000")))
                    .isEqualByComparingTo(timeline.calculateRuleFollowedAsset(new BigDecimal("950000"), DAY_2));
        }

        @Test
        @DisplayName("일별 자산이 없으면 배수를 만들 수 없어 금액만 얹는다")
        void calculateFinalAsset_withoutAssetTimeline_addsAmount() {
            RuleFollowedAssetTimeline timeline = RuleFollowedAssetTimeline.build(
                    List.of(unrealizedOn(DAY_1, "50000"), realizedOn(DAY_1, DAY_2, "20000")),
                    List.of(),
                    AssetTimeline.of(List.of()));

            assertThat(timeline.calculateFinalAsset(new BigDecimal("1000000")))
                    .isEqualByComparingTo(new BigDecimal("1070000"));
        }
    }
}
