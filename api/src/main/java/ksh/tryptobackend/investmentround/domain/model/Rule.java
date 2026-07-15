package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;

public sealed interface Rule {

    BigDecimal ZERO = BigDecimal.ZERO;
    BigDecimal MIN_COUNT = BigDecimal.ONE;

    Long id();

    RuleType ruleType();

    BigDecimal thresholdValue();

    LocalDateTime createdAt();

    void validateThreshold();

    Optional<DetectedViolation> check(RuleEvaluationInput context);

    static Rule create(RuleType ruleType, BigDecimal thresholdValue, LocalDateTime createdAt) {
        Rule rule = of(null, ruleType, thresholdValue, createdAt);
        rule.validateThreshold();
        return rule;
    }

    static Rule of(Long id, RuleType ruleType, BigDecimal thresholdValue, LocalDateTime createdAt) {
        return switch (ruleType) {
            case LOSS_CUT -> new LossCutRule(id, thresholdValue, createdAt);
            case PROFIT_TAKE -> new ProfitTakeRule(id, thresholdValue, createdAt);
            case CHASE_BUY_BAN -> new ChaseBuyBanRule(id, thresholdValue, createdAt);
            case AVERAGING_DOWN_LIMIT -> new AveragingDownLimitRule(id, thresholdValue, createdAt);
            case OVERTRADING_LIMIT -> new OvertradingLimitRule(id, thresholdValue, createdAt);
        };
    }

    static void validateRate(BigDecimal thresholdValue) {
        if (thresholdValue.compareTo(ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_RULE_THRESHOLD);
        }
    }

    static void validateCount(BigDecimal thresholdValue) {
        if (thresholdValue.compareTo(MIN_COUNT) < 0
                || thresholdValue.stripTrailingZeros().scale() > 0) {
            throw new CustomException(ErrorCode.INVALID_RULE_THRESHOLD);
        }
    }

    record LossCutRule(Long id, BigDecimal thresholdValue, LocalDateTime createdAt) implements Rule {

        @Override
        public RuleType ruleType() {
            return RuleType.LOSS_CUT;
        }

        @Override
        public void validateThreshold() {
            Rule.validateRate(thresholdValue);
        }

        @Override
        public Optional<DetectedViolation> check(RuleEvaluationInput context) {
            return Optional.empty();
        }
    }

    record ProfitTakeRule(Long id, BigDecimal thresholdValue, LocalDateTime createdAt) implements Rule {

        @Override
        public RuleType ruleType() {
            return RuleType.PROFIT_TAKE;
        }

        @Override
        public void validateThreshold() {
            Rule.validateRate(thresholdValue);
        }

        @Override
        public Optional<DetectedViolation> check(RuleEvaluationInput context) {
            return Optional.empty();
        }
    }

    record ChaseBuyBanRule(Long id, BigDecimal thresholdValue, LocalDateTime createdAt) implements Rule {

        @Override
        public RuleType ruleType() {
            return RuleType.CHASE_BUY_BAN;
        }

        @Override
        public void validateThreshold() {
            Rule.validateRate(thresholdValue);
        }

        @Override
        public Optional<DetectedViolation> check(RuleEvaluationInput context) {
            if (!context.buyOrder()) {
                return Optional.empty();
            }
            if (context.changeRate().compareTo(thresholdValue) < 0) {
                return Optional.empty();
            }
            String reason = String.format("상승률 %s%% ≥ %s%%", context.changeRate(), thresholdValue);
            return Optional.of(new DetectedViolation(id, reason, context.now()));
        }
    }

    record AveragingDownLimitRule(Long id, BigDecimal thresholdValue, LocalDateTime createdAt) implements Rule {

        @Override
        public RuleType ruleType() {
            return RuleType.AVERAGING_DOWN_LIMIT;
        }

        @Override
        public void validateThreshold() {
            Rule.validateCount(thresholdValue);
        }

        @Override
        public Optional<DetectedViolation> check(RuleEvaluationInput context) {
            if (!context.buyOrder()) {
                return Optional.empty();
            }
            if (!context.atLoss()) {
                return Optional.empty();
            }
            int maxCount = thresholdValue.intValue();
            int newCount = context.averagingDownCount() + 1;
            if (newCount <= maxCount) {
                return Optional.empty();
            }
            String reason = String.format("물타기 %d회 > %d회", newCount, maxCount);
            return Optional.of(new DetectedViolation(id, reason, context.now()));
        }
    }

    record OvertradingLimitRule(Long id, BigDecimal thresholdValue, LocalDateTime createdAt) implements Rule {

        @Override
        public RuleType ruleType() {
            return RuleType.OVERTRADING_LIMIT;
        }

        @Override
        public void validateThreshold() {
            Rule.validateCount(thresholdValue);
        }

        @Override
        public Optional<DetectedViolation> check(RuleEvaluationInput context) {
            long maxOrderCount = thresholdValue.longValue();
            long todayOrderCount = context.todayOrderCount();
            if (todayOrderCount <= maxOrderCount) {
                return Optional.empty();
            }
            String reason = String.format("오늘 주문 %d건 > %d건", todayOrderCount, maxOrderCount);
            return Optional.of(new DetectedViolation(id, reason, context.now()));
        }
    }
}
