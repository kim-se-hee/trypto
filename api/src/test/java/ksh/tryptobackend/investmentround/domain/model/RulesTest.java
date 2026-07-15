package ksh.tryptobackend.investmentround.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 26, 10, 0);

    @Nested
    @DisplayName("추격 매수 금지")
    class ChaseBuyBanTest {

        @Test
        @DisplayName("매수 + 상승률 ≥ 설정값 → 위반")
        void buyWithHighChangeRate_violation() {
            Rule rule = Rule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, new BigDecimal("5"), false, 0, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("매수 + 상승률 < 설정값 → 위반 없음")
        void buyWithLowChangeRate_noViolation() {
            Rule rule = Rule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, new BigDecimal("4.9"), false, 0, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문 → 추격 매수 체크 스킵")
        void sellOrder_skipped() {
            Rule rule = Rule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(false, new BigDecimal("10"), false, 0, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("물타기 제한")
    class AveragingDownLimitTest {

        @Test
        @DisplayName("매수 + 손실 중 + 이번 물타기가 설정값 초과 → 위반")
        void buyAtLossExceedingLimit_violation() {
            Rule rule = Rule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, true, 3, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("매수 + 손실 중 + 이번 물타기가 설정값 이내 → 위반 없음")
        void buyAtLossBelowLimit_noViolation() {
            Rule rule = Rule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, true, 2, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매수 + 이익 중 → 물타기 아님")
        void buyAtProfit_noViolation() {
            Rule rule = Rule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("1"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, false, 5, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("보유 없음 → 물타기 체크 스킵")
        void noHolding_skipped() {
            Rule rule = Rule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("1"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, false, 0, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("과매매 제한")
    class OvertradingLimitTest {

        @Test
        @DisplayName("오늘 주문 건수(현재 주문 포함) > 설정값 → 위반")
        void orderCountExceedingLimit_violation() {
            Rule rule = Rule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, false, 0, 11, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("오늘 주문 건수(현재 주문 포함) = 설정값 → 위반 없음")
        void orderCountBelowLimit_noViolation() {
            Rule rule = Rule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, false, 0, 10, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문도 과매매 체크 대상")
        void sellOrder_alsoChecked() {
            Rule rule = Rule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("5"), NOW);
            RuleEvaluationInput context = new RuleEvaluationInput(false, BigDecimal.ZERO, false, 0, 6, NOW);

            List<DetectedViolation> violations = new Rules(List.of(rule)).check(context);

            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("복합 규칙")
    class MultipleRulesTest {

        @Test
        @DisplayName("여러 규칙 동시 위반 — 모두 기록")
        void multipleViolations_allRecorded() {
            List<Rule> rules = List.of(
                    Rule.of(1L, RuleType.CHASE_BUY_BAN, new BigDecimal("5"), NOW),
                    Rule.of(2L, RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"), NOW),
                    Rule.of(3L, RuleType.OVERTRADING_LIMIT, new BigDecimal("10"), NOW));
            RuleEvaluationInput context = new RuleEvaluationInput(true, new BigDecimal("10"), true, 3, 11, NOW);

            List<DetectedViolation> violations = new Rules(rules).check(context);

            assertThat(violations).hasSize(3);
        }

        @Test
        @DisplayName("규칙 없음 → 빈 리스트")
        void noRules_emptyList() {
            RuleEvaluationInput context = new RuleEvaluationInput(true, BigDecimal.ZERO, false, 0, 0, NOW);

            List<DetectedViolation> violations = new Rules(List.of()).check(context);

            assertThat(violations).isEmpty();
        }
    }
}
