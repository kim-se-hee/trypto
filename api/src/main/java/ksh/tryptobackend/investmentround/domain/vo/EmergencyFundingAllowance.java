package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record EmergencyFundingAllowance(BigDecimal limit, int remainingCount) {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_LIMIT = new BigDecimal("1000000");
    private static final int DEFAULT_CHARGE_COUNT = 3;

    public static EmergencyFundingAllowance initial(BigDecimal limit) {
        if (limit.compareTo(ZERO) < 0 || limit.compareTo(MAX_LIMIT) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
        }
        return new EmergencyFundingAllowance(limit, DEFAULT_CHARGE_COUNT);
    }

    public static EmergencyFundingAllowance of(BigDecimal limit, int remainingCount) {
        return new EmergencyFundingAllowance(limit, remainingCount);
    }

    public void validateChargeable(BigDecimal amount) {
        if (limit.compareTo(ZERO) == 0) {
            throw new CustomException(ErrorCode.EMERGENCY_FUNDING_DISABLED);
        }
        if (remainingCount <= 0) {
            throw new CustomException(ErrorCode.EMERGENCY_FUNDING_CHANCE_EXHAUSTED);
        }
        if (amount.compareTo(ZERO) <= 0 || amount.compareTo(limit) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_AMOUNT);
        }
    }

    public EmergencyFundingAllowance consume() {
        return new EmergencyFundingAllowance(limit, remainingCount - 1);
    }
}
