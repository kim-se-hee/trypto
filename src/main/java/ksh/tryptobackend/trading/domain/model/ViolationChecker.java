package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.application.port.out.HoldingPort.HoldingData;
import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort.InvestmentRuleData;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ViolationChecker {

    private static final String CHASE_BUY_BAN = "CHASE_BUY_BAN";
    private static final String AVERAGING_LIMIT = "AVERAGING_LIMIT";
    private static final String OVERTRADING_LIMIT = "OVERTRADING_LIMIT";

    public static List<RuleViolation> check(Order order, List<InvestmentRuleData> rules,
                                            HoldingData holding, BigDecimal changeRate,
                                            BigDecimal currentPrice, long todayOrderCount,
                                            LocalDateTime now) {
        return rules.stream()
            .map(rule -> checkRule(order, rule, holding, changeRate, currentPrice, todayOrderCount, now))
            .flatMap(Optional::stream)
            .toList();
    }

    private static Optional<RuleViolation> checkRule(Order order, InvestmentRuleData rule,
                                                     HoldingData holding, BigDecimal changeRate,
                                                     BigDecimal currentPrice, long todayOrderCount,
                                                     LocalDateTime now) {
        return switch (rule.ruleType()) {
            case CHASE_BUY_BAN -> checkChaseBuyBan(order, rule, changeRate, now);
            case AVERAGING_LIMIT -> checkAveragingLimit(order, rule, holding, currentPrice, now);
            case OVERTRADING_LIMIT -> checkOvertradingLimit(rule, todayOrderCount, now);
            default -> Optional.empty();
        };
    }

    private static Optional<RuleViolation> checkChaseBuyBan(Order order, InvestmentRuleData rule,
                                                            BigDecimal changeRate, LocalDateTime now) {
        if (order.getSide() != Side.BUY) {
            return Optional.empty();
        }
        if (changeRate.compareTo(rule.thresholdValue()) < 0) {
            return Optional.empty();
        }
        String reason = String.format("상승률 %s%% ≥ %s%%", changeRate, rule.thresholdValue());
        return Optional.of(new RuleViolation(rule.ruleId(), reason, now));
    }

    private static Optional<RuleViolation> checkAveragingLimit(Order order, InvestmentRuleData rule,
                                                               HoldingData holding,
                                                               BigDecimal currentPrice, LocalDateTime now) {
        if (order.getSide() != Side.BUY) {
            return Optional.empty();
        }
        if (holding == null) {
            return Optional.empty();
        }
        if (holding.avgBuyPrice().compareTo(currentPrice) <= 0) {
            return Optional.empty();
        }
        int newCount = holding.averagingDownCount() + 1;
        if (newCount < rule.thresholdValue().intValue()) {
            return Optional.empty();
        }
        String reason = String.format("물타기 %d회 ≥ %d회", newCount, rule.thresholdValue().intValue());
        return Optional.of(new RuleViolation(rule.ruleId(), reason, now));
    }

    private static Optional<RuleViolation> checkOvertradingLimit(InvestmentRuleData rule,
                                                                 long todayOrderCount, LocalDateTime now) {
        long newCount = todayOrderCount + 1;
        if (newCount < rule.thresholdValue().longValue()) {
            return Optional.empty();
        }
        String reason = String.format("오늘 주문 %d건 ≥ %d건", newCount, rule.thresholdValue().intValue());
        return Optional.of(new RuleViolation(rule.ruleId(), reason, now));
    }
}
