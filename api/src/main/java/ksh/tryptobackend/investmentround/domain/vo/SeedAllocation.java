package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;

public record SeedAllocation(Long exchangeId, Long baseCurrencyCoinId, BigDecimal amount) {

    public static SeedAllocation create(Long exchangeId, SeedFundingSpec spec, BigDecimal amount) {
        spec.seedAmountPolicy().validate(amount);
        return new SeedAllocation(exchangeId, spec.baseCurrencyCoinId(), amount);
    }
}
