package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.application.port.out.HoldingPort.HoldingData;
import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort.InvestmentRuleData;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderAmountPolicy;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ViolationCheckerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 26, 10, 0);
    private static final TradingVenue VENUE = new TradingVenue(
        new BigDecimal("0.0005"), 1L, OrderAmountPolicy.DOMESTIC);

    @Nested
    @DisplayName("추격 매수 금지")
    class ChaseBuyBanTest {

        @Test
        @DisplayName("매수 + 상승률 ≥ 설정값 → 위반")
        void buyWithHighChangeRate_violation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(1L, "CHASE_BUY_BAN", new BigDecimal("5"));

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, new BigDecimal("5"), new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("매수 + 상승률 < 설정값 → 위반 없음")
        void buyWithLowChangeRate_noViolation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(1L, "CHASE_BUY_BAN", new BigDecimal("5"));

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, new BigDecimal("4.9"), new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문 → 추격 매수 체크 스킵")
        void sellOrder_skipped() {
            Order order = createSellOrder();
            InvestmentRuleData rule = new InvestmentRuleData(1L, "CHASE_BUY_BAN", new BigDecimal("5"));

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, new BigDecimal("10"), new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("물타기 제한")
    class AveragingLimitTest {

        @Test
        @DisplayName("매수 + 손실 중 + 물타기 횟수 ≥ 설정값 → 위반")
        void buyAtLossExceedingLimit_violation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(2L, "AVERAGING_LIMIT", new BigDecimal("3"));
            HoldingData holding = new HoldingData(new BigDecimal("60000000"), new BigDecimal("0.01"), 2);

            // 현재가 50000000 < 평균 매수가 60000000 → 손실 중, 새 카운트 = 2 + 1 = 3 ≥ 3
            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), holding, BigDecimal.ZERO, new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("매수 + 손실 중 + 물타기 횟수 < 설정값 → 위반 없음")
        void buyAtLossBelowLimit_noViolation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(2L, "AVERAGING_LIMIT", new BigDecimal("3"));
            HoldingData holding = new HoldingData(new BigDecimal("60000000"), new BigDecimal("0.01"), 1);

            // 새 카운트 = 1 + 1 = 2 < 3
            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), holding, BigDecimal.ZERO, new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매수 + 이익 중 → 물타기 아님")
        void buyAtProfit_noViolation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(2L, "AVERAGING_LIMIT", new BigDecimal("1"));
            HoldingData holding = new HoldingData(new BigDecimal("40000000"), new BigDecimal("0.01"), 5);

            // 현재가 50000000 > 평균 매수가 40000000 → 이익 중
            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), holding, BigDecimal.ZERO, new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("보유 없음 → 물타기 체크 스킵")
        void noHolding_skipped() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(2L, "AVERAGING_LIMIT", new BigDecimal("1"));

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, BigDecimal.ZERO, new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("과매매 제한")
    class OvertradingLimitTest {

        @Test
        @DisplayName("오늘 주문 건수 + 1 ≥ 설정값 → 위반")
        void orderCountExceedingLimit_violation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(3L, "OVERTRADING_LIMIT", new BigDecimal("10"));

            // todayOrderCount = 9, 새 카운트 = 9 + 1 = 10 ≥ 10
            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, BigDecimal.ZERO, new BigDecimal("50000000"), 9, NOW);

            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).ruleId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("오늘 주문 건수 + 1 < 설정값 → 위반 없음")
        void orderCountBelowLimit_noViolation() {
            Order order = createBuyOrder();
            InvestmentRuleData rule = new InvestmentRuleData(3L, "OVERTRADING_LIMIT", new BigDecimal("10"));

            // todayOrderCount = 8, 새 카운트 = 8 + 1 = 9 < 10
            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, BigDecimal.ZERO, new BigDecimal("50000000"), 8, NOW);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("매도 주문도 과매매 체크 대상")
        void sellOrder_alsoChecked() {
            Order order = createSellOrder();
            InvestmentRuleData rule = new InvestmentRuleData(3L, "OVERTRADING_LIMIT", new BigDecimal("5"));

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(rule), null, BigDecimal.ZERO, new BigDecimal("50000000"), 4, NOW);

            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("복합 규칙")
    class MultipleRulesTest {

        @Test
        @DisplayName("여러 규칙 동시 위반 — 모두 기록")
        void multipleViolations_allRecorded() {
            Order order = createBuyOrder();
            HoldingData holding = new HoldingData(new BigDecimal("60000000"), new BigDecimal("0.01"), 2);
            List<InvestmentRuleData> rules = List.of(
                new InvestmentRuleData(1L, "CHASE_BUY_BAN", new BigDecimal("5")),
                new InvestmentRuleData(2L, "AVERAGING_LIMIT", new BigDecimal("3")),
                new InvestmentRuleData(3L, "OVERTRADING_LIMIT", new BigDecimal("10"))
            );

            List<RuleViolation> violations = ViolationChecker.check(
                order, rules, holding, new BigDecimal("10"), new BigDecimal("50000000"), 9, NOW);

            assertThat(violations).hasSize(3);
        }

        @Test
        @DisplayName("규칙 없음 → 빈 리스트")
        void noRules_emptyList() {
            Order order = createBuyOrder();

            List<RuleViolation> violations = ViolationChecker.check(
                order, List.of(), null, BigDecimal.ZERO, new BigDecimal("50000000"), 0, NOW);

            assertThat(violations).isEmpty();
        }
    }

    private Order createBuyOrder() {
        return Order.createMarketBuyOrder(
            UUID.randomUUID().toString(), 1L, 1L,
            new BigDecimal("100000"), new BigDecimal("50000000"), VENUE, NOW);
    }

    private Order createSellOrder() {
        return Order.createMarketSellOrder(
            UUID.randomUUID().toString(), 1L, 1L,
            new BigDecimal("0.01"), new BigDecimal("50000000"), VENUE, NOW);
    }
}
