package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeedAmountPolicy {
    CASH_INFLOW(new BigDecimal("1000000"), new BigDecimal("50000000")),
    NO_CASH_INFLOW(BigDecimal.ZERO, BigDecimal.ZERO);

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public static SeedAmountPolicy forCashInflow(boolean cashInflow) {
        return cashInflow ? CASH_INFLOW : NO_CASH_INFLOW;
    }

    public void validate(BigDecimal amount) {
        if (amount.compareTo(ZERO) < 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
        if (amount.compareTo(ZERO) == 0) {
            return;
        }
        if (this == NO_CASH_INFLOW) {
            throw new CustomException(ErrorCode.SEED_NOT_ALLOWED_FOR_EXCHANGE);
        }
        if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
    }
}
