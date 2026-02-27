package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class Exchange {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DOMESTIC_MIN_SEED = new BigDecimal("1000000");
    private static final BigDecimal DOMESTIC_MAX_SEED = new BigDecimal("50000000");
    private static final BigDecimal OVERSEAS_MIN_SEED = new BigDecimal("100");
    private static final BigDecimal OVERSEAS_MAX_SEED = new BigDecimal("50000");

    private final Long exchangeId;
    private final String name;
    private final ExchangeMarketType marketType;
    private final Long baseCurrencyCoinId;

    public void validateSeedAmount(BigDecimal amount) {
        if (amount.compareTo(ZERO) < 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
        if (amount.compareTo(ZERO) == 0) {
            return;
        }

        if (marketType == ExchangeMarketType.DOMESTIC) {
            validateRange(amount, DOMESTIC_MIN_SEED, DOMESTIC_MAX_SEED);
            return;
        }
        validateRange(amount, OVERSEAS_MIN_SEED, OVERSEAS_MAX_SEED);
    }

    private void validateRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
        }
    }
}
