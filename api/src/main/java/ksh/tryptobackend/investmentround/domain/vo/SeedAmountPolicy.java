package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeedAmountPolicy {
    KRW_SEED(new BigDecimal("1000000"), new BigDecimal("50000000")),
    USDT_SEED(new BigDecimal("1000"), new BigDecimal("30000"));

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    public static SeedAmountPolicy forDomestic(boolean domestic) {
        return domestic ? KRW_SEED : USDT_SEED;
    }

    // 0 은 "이 거래소엔 시드를 넣지 않음"이라 허용한다. 넣는 경우에만 한도를 검사한다.
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
