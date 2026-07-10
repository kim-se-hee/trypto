package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeedAmountPolicy {
    DOMESTIC(new BigDecimal("1000000"), new BigDecimal("50000000")),
    OVERSEAS(new BigDecimal("100"), new BigDecimal("50000"));

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public static SeedAmountPolicy forDomestic(boolean domestic) {
        return domestic ? DOMESTIC : OVERSEAS;
    }

    public void validate(BigDecimal amount) {
        if (amount.compareTo(ZERO) < 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
        if (amount.compareTo(ZERO) == 0) {
            return;
        }
        if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
    }
}
