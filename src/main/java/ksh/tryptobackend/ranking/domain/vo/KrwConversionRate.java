package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public enum KrwConversionRate {

    DOMESTIC(BigDecimal.ONE),
    OVERSEAS(new BigDecimal("1400"));

    private final BigDecimal rate;

    KrwConversionRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal convert(BigDecimal amount) {
        return amount.multiply(rate);
    }
}
