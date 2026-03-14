package ksh.tryptobackend.investmentround.domain.model;

import ksh.tryptobackend.common.domain.vo.RuleType;

import java.math.BigDecimal;
import java.util.Optional;

public sealed interface ViolationRule {

    Long ruleId();

    Optional<DetectedViolation> check(ViolationCheckContext context);

    static ViolationRule of(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {
        return switch (ruleType) {
            case LOSS_CUT -> new LossCutRule(ruleId, thresholdValue);
            case PROFIT_TAKE -> new ProfitTakeRule(ruleId, thresholdValue);
            case CHASE_BUY_BAN -> new ChaseBuyBanRule(ruleId, thresholdValue);
            case AVERAGING_DOWN_LIMIT -> new AveragingDownLimitRule(ruleId, thresholdValue.intValue());
            case OVERTRADING_LIMIT -> new OvertradingLimitRule(ruleId, thresholdValue.longValue());
        };
    }

    record LossCutRule(Long ruleId, BigDecimal thresholdPercent) implements ViolationRule {

        @Override
        public Optional<DetectedViolation> check(ViolationCheckContext context) {
            return Optional.empty();
        }
    }

    record ProfitTakeRule(Long ruleId, BigDecimal thresholdPercent) implements ViolationRule {

        @Override
        public Optional<DetectedViolation> check(ViolationCheckContext context) {
            return Optional.empty();
        }
    }

    record ChaseBuyBanRule(Long ruleId, BigDecimal thresholdPercent) implements ViolationRule {

        @Override
        public Optional<DetectedViolation> check(ViolationCheckContext context) {
            if (!context.buyOrder()) {
                return Optional.empty();
            }
            if (context.changeRate().compareTo(thresholdPercent) < 0) {
                return Optional.empty();
            }
            String reason = String.format("상승률 %s%% ≥ %s%%", context.changeRate(), thresholdPercent);
            return Optional.of(new DetectedViolation(ruleId, reason, context.now()));
        }
    }

    record AveragingDownLimitRule(Long ruleId, int maxCount) implements ViolationRule {

        @Override
        public Optional<DetectedViolation> check(ViolationCheckContext context) {
            if (!context.buyOrder()) {
                return Optional.empty();
            }
            if (!context.isHolding()) {
                return Optional.empty();
            }
            if (!context.isAtLoss()) {
                return Optional.empty();
            }
            int newCount = context.averagingDownCount() + 1;
            if (newCount < maxCount) {
                return Optional.empty();
            }
            String reason = String.format("물타기 %d회 ≥ %d회", newCount, maxCount);
            return Optional.of(new DetectedViolation(ruleId, reason, context.now()));
        }
    }

    record OvertradingLimitRule(Long ruleId, long maxOrderCount) implements ViolationRule {

        @Override
        public Optional<DetectedViolation> check(ViolationCheckContext context) {
            long newCount = context.todayOrderCount() + 1;
            if (newCount < maxOrderCount) {
                return Optional.empty();
            }
            String reason = String.format("오늘 주문 %d건 ≥ %d건", newCount, maxOrderCount);
            return Optional.of(new DetectedViolation(ruleId, reason, context.now()));
        }
    }
}
