package ksh.tryptobackend.regretanalysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CumulativeLossTimelineTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);
    private static final LocalDate DAY_4 = LocalDate.of(2025, 1, 4);

    private ViolationDetail violationOn(LocalDate date, BigDecimal lossAmount) {
        return ViolationDetail.create(1L, 1L, 1L, lossAmount, BigDecimal.ZERO, date.atStartOfDay());
    }

    @Nested
    @DisplayName("누적 손실 타임라인 생성")
    class BuildTest {

        @Test
        @DisplayName("위반이 없으면 모든 날짜의 누적 손실은 0이다")
        void build_noViolations_allZero() {
            // Given
            List<ViolationDetail> violations = List.of();
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, dates);

            // Then
            assertThat(timeline.getLossAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(timeline.getLossAt(DAY_2)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(timeline.getLossAt(DAY_3)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("위반 손실이 발생일부터 이후 날짜까지 누적된다")
        void build_violationsAccumulate_lossCarriedForward() {
            // Given
            List<ViolationDetail> violations = List.of(violationOn(DAY_2, new BigDecimal("10000")));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, dates);

            // Then
            assertThat(timeline.getLossAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(timeline.getLossAt(DAY_2)).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(timeline.getLossAt(DAY_3)).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("여러 위반이 날짜순으로 누적된다")
        void build_multipleViolations_cumulativeSum() {
            // Given
            List<ViolationDetail> violations =
                    List.of(violationOn(DAY_1, new BigDecimal("5000")), violationOn(DAY_3, new BigDecimal("15000")));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3, DAY_4);

            // When
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, dates);

            // Then
            assertThat(timeline.getLossAt(DAY_1)).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(timeline.getLossAt(DAY_2)).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(timeline.getLossAt(DAY_3)).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(timeline.getLossAt(DAY_4)).isEqualByComparingTo(new BigDecimal("20000"));
        }

        @Test
        @DisplayName("같은 날짜에 여러 위반이 있으면 모두 합산된다")
        void build_multipleViolationsSameDay_allSummed() {
            // Given
            List<ViolationDetail> violations =
                    List.of(violationOn(DAY_2, new BigDecimal("3000")), violationOn(DAY_2, new BigDecimal("7000")));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, dates);

            // Then
            assertThat(timeline.getLossAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(timeline.getLossAt(DAY_2)).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(timeline.getLossAt(DAY_3)).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("위반이 정렬되지 않은 상태로 전달되어도 올바르게 누적된다")
        void build_unsortedViolations_correctlyAccumulated() {
            // Given
            List<ViolationDetail> violations =
                    List.of(violationOn(DAY_3, new BigDecimal("20000")), violationOn(DAY_1, new BigDecimal("5000")));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, dates);

            // Then
            assertThat(timeline.getLossAt(DAY_1)).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(timeline.getLossAt(DAY_2)).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(timeline.getLossAt(DAY_3)).isEqualByComparingTo(new BigDecimal("25000"));
        }
    }

    @Nested
    @DisplayName("원칙 준수 자산 계산")
    class CalculateRuleFollowedAssetTest {

        @Test
        @DisplayName("실제 자산에 누적 손실을 더하여 원칙 준수 자산을 계산한다")
        void calculateRuleFollowedAsset_addsLossToActual() {
            // Given
            List<ViolationDetail> violations = List.of(violationOn(DAY_1, new BigDecimal("50000")));
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(violations, List.of(DAY_1, DAY_2));
            BigDecimal actualAsset = new BigDecimal("900000");

            // When
            BigDecimal ruleFollowed = timeline.calculateRuleFollowedAsset(actualAsset, DAY_2);

            // Then
            assertThat(ruleFollowed).isEqualByComparingTo(new BigDecimal("950000"));
        }

        @Test
        @DisplayName("누적 손실이 0이면 원칙 준수 자산은 실제 자산과 같다")
        void calculateRuleFollowedAsset_noLoss_equalsActual() {
            // Given
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(List.of(), List.of(DAY_1));
            BigDecimal actualAsset = new BigDecimal("1000000");

            // When
            BigDecimal ruleFollowed = timeline.calculateRuleFollowedAsset(actualAsset, DAY_1);

            // Then
            assertThat(ruleFollowed).isEqualByComparingTo(actualAsset);
        }
    }

    @Nested
    @DisplayName("특정 날짜 손실 조회")
    class GetLossAtTest {

        @Test
        @DisplayName("타임라인에 없는 날짜를 조회하면 0을 반환한다")
        void getLossAt_nonExistentDate_returnsZero() {
            // Given
            CumulativeLossTimeline timeline = CumulativeLossTimeline.build(List.of(), List.of(DAY_1));

            // When
            BigDecimal loss = timeline.getLossAt(DAY_3);

            // Then
            assertThat(loss).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
