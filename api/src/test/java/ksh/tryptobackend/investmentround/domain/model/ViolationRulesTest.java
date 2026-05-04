package ksh.tryptobackend.investmentround.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.domain.vo.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ViolationRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 26, 10, 0);

    @Nested
    @DisplayName("추격 매수 금지")
    class ChaseBuyBanTest {

        @Test
        @DisplayName("매수 + 상승률 ≥ 설정값 → 위반")
        void buyWithHighChangeRate_violation() {
            ViolationRule rule = ViolationRule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            new BigDecimal("5"),
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("매수 + 상승률 < 설정값 → 위반 없음")
        void buyWithLowChangeRate_noViolation() {
            ViolationRule rule = ViolationRule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            new BigDecimal("4.9"),
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문 → 추격 매수 체크 스킵")
        void sellOrder_skipped() {
            ViolationRule rule = ViolationRule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            false,
                            new BigDecimal("10"),
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("물타기 제한")
    class AveragingDownLimitTest {

        @Test
        @DisplayName("매수 + 손실 중 + 물타기 횟수 ≥ 설정값 → 위반")
        void buyAtLossExceedingLimit_violation() {
            ViolationRule rule =
                    ViolationRule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            new BigDecimal("60000000"),
                            new BigDecimal("0.01"),
                            2,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            // 현재가 50000000 < 평균 매수가 60000000 → 손실 중, 새 카운트 = 2 + 1 = 3 ≥ 3
            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("매수 + 손실 중 + 물타기 횟수 < 설정값 → 위반 없음")
        void buyAtLossBelowLimit_noViolation() {
            ViolationRule rule =
                    ViolationRule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            new BigDecimal("60000000"),
                            new BigDecimal("0.01"),
                            1,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            // 새 카운트 = 1 + 1 = 2 < 3
            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매수 + 이익 중 → 물타기 아님")
        void buyAtProfit_noViolation() {
            ViolationRule rule =
                    ViolationRule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("1"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            new BigDecimal("40000000"),
                            new BigDecimal("0.01"),
                            5,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            // 현재가 50000000 > 평균 매수가 40000000 → 이익 중
            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("보유 없음 → 물타기 체크 스킵")
        void noHolding_skipped() {
            ViolationRule rule =
                    ViolationRule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("1"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("과매매 제한")
    class OvertradingLimitTest {

        @Test
        @DisplayName("오늘 주문 건수 + 1 ≥ 설정값 → 위반")
        void orderCountExceedingLimit_violation() {
            ViolationRule rule =
                    ViolationRule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            9,
                            NOW);

            // todayOrderCount = 9, 새 카운트 = 9 + 1 = 10 ≥ 10
            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("오늘 주문 건수 + 1 < 설정값 → 위반 없음")
        void orderCountBelowLimit_noViolation() {
            ViolationRule rule =
                    ViolationRule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            8,
                            NOW);

            // todayOrderCount = 8, 새 카운트 = 8 + 1 = 9 < 10
            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문도 과매매 체크 대상")
        void sellOrder_alsoChecked() {
            ViolationRule rule =
                    ViolationRule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("5"));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            false,
                            BigDecimal.ZERO,
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            4,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("복합 규칙")
    class MultipleRulesTest {

        @Test
        @DisplayName("여러 규칙 동시 위반 — 모두 기록")
        void multipleViolations_allRecorded() {
            List<ViolationRule> rules =
                    List.of(
                            ViolationRule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5")),
                            ViolationRule.of(
                                    2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3")),
                            ViolationRule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10")));
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            new BigDecimal("10"),
                            new BigDecimal("60000000"),
                            new BigDecimal("0.01"),
                            2,
                            new BigDecimal("50000000"),
                            9,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(rules).check(context);

            assertThat(violations).hasSize(3);
        }

        @Test
        @DisplayName("규칙 없음 → 빈 리스트")
        void noRules_emptyList() {
            ViolationCheckContext context =
                    new ViolationCheckContext(
                            true,
                            BigDecimal.ZERO,
                            null,
                            null,
                            0,
                            new BigDecimal("50000000"),
                            0,
                            NOW);

            List<DetectedViolation> violations = new ViolationRules(List.of()).check(context);

            assertThat(violations).isEmpty();
        }
    }
}
