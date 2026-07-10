package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

/** 긴급 충전 한도와 남은 충전 횟수를 한 단위로 묶은 값. 한도 검증·충전 가능 여부 판정·횟수 차감을 책임진다. */
public record EmergencyFundingAllowance(BigDecimal limit, int remainingCount) {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_LIMIT = new BigDecimal("1000000");
    private static final int DEFAULT_CHARGE_COUNT = 3;

    /** 라운드 시작 시점의 초기 충전 권한. 한도를 검증하고 기본 횟수를 부여한다. */
    public static EmergencyFundingAllowance initial(BigDecimal limit) {
        if (limit.compareTo(ZERO) < 0 || limit.compareTo(MAX_LIMIT) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
        }
        return new EmergencyFundingAllowance(limit, DEFAULT_CHARGE_COUNT);
    }

    /** 영속 상태로부터의 복원. 이미 검증된 값이므로 검증하지 않는다. */
    public static EmergencyFundingAllowance of(BigDecimal limit, int remainingCount) {
        return new EmergencyFundingAllowance(limit, remainingCount);
    }

    /** 주어진 금액으로 충전 가능한지 검증한다. 불가하면 사유별 예외를 던진다. */
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

    /** 한 번 충전한 뒤의 권한. 남은 횟수가 하나 줄어든다. */
    public EmergencyFundingAllowance consume() {
        return new EmergencyFundingAllowance(limit, remainingCount - 1);
    }
}
