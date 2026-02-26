package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.domain.vo.RuleType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.util.Optional;

public sealed interface InvestmentRule {

    Long ruleId();

    Optional<RuleViolation> check(ViolationCheckContext context);

    static InvestmentRule of(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {
        return switch (ruleType) {
            case CHASE_BUY_BAN -> new ChaseBuyBanRule(ruleId, thresholdValue);
            case AVERAGING_DOWN_LIMIT -> new AveragingDownLimitRule(ruleId, thresholdValue.intValue());
            case OVERTRADING_LIMIT -> new OvertradingLimitRule(ruleId, thresholdValue.longValue());
        };
    }

    record ChaseBuyBanRule(Long ruleId, BigDecimal thresholdPercent) implements InvestmentRule {

        @Override
        public Optional<RuleViolation> check(ViolationCheckContext context) {
            if (context.orderSide() != Side.BUY) {
                return Optional.empty();
            }
            if (context.changeRate().compareTo(thresholdPercent) < 0) {
                return Optional.empty();
            }
            String reason = String.format("상승률 %s%% ≥ %s%%", context.changeRate(), thresholdPercent);
            return Optional.of(new RuleViolation(ruleId, reason, context.now()));
        }
    }

    record AveragingDownLimitRule(Long ruleId, int maxCount) implements InvestmentRule {

        @Override
        public Optional<RuleViolation> check(ViolationCheckContext context) {
            if (context.orderSide() != Side.BUY) {
                return Optional.empty();
            }
            if (context.holding() == null) {
                return Optional.empty();
            }
            if (!context.holding().isAtLoss(context.currentPrice())) {
                return Optional.empty();
            }
            int newCount = context.holding().getAveragingDownCount() + 1;
            if (newCount < maxCount) {
                return Optional.empty();
            }
            String reason = String.format("물타기 %d회 ≥ %d회", newCount, maxCount);
            return Optional.of(new RuleViolation(ruleId, reason, context.now()));
        }
    }

    record OvertradingLimitRule(Long ruleId, long maxOrderCount) implements InvestmentRule {

        @Override
        public Optional<RuleViolation> check(ViolationCheckContext context) {
            long newCount = context.todayOrderCount() + 1;
            if (newCount < maxOrderCount) {
                return Optional.empty();
            }
            String reason = String.format("오늘 주문 %d건 ≥ %d건", newCount, maxOrderCount);
            return Optional.of(new RuleViolation(ruleId, reason, context.now()));
        }
    }
}
