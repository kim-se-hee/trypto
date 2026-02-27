package ksh.tryptobackend.investmentround.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.domain.vo.RuleType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvestmentRule {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MIN_COUNT = BigDecimal.ONE;

    private final Long ruleId;
    private final Long roundId;
    private final RuleType ruleType;
    private final BigDecimal thresholdValue;
    private final LocalDateTime createdAt;

    public static InvestmentRule create(Long roundId, RuleType ruleType, BigDecimal thresholdValue, LocalDateTime createdAt) {
        validateThreshold(ruleType, thresholdValue);
        return InvestmentRule.builder()
            .roundId(roundId)
            .ruleType(ruleType)
            .thresholdValue(thresholdValue)
            .createdAt(createdAt)
            .build();
    }

    public static void validateThreshold(RuleType ruleType, BigDecimal thresholdValue) {
        if (isRateRule(ruleType)) {
            if (thresholdValue.compareTo(ZERO) <= 0) {
                throw new CustomException(ErrorCode.INVALID_RULE_THRESHOLD);
            }
            return;
        }

        if (thresholdValue.compareTo(MIN_COUNT) < 0 || thresholdValue.stripTrailingZeros().scale() > 0) {
            throw new CustomException(ErrorCode.INVALID_RULE_THRESHOLD);
        }
    }

    private static boolean isRateRule(RuleType ruleType) {
        return switch (ruleType) {
            case LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN -> true;
            case AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT -> false;
        };
    }
}
