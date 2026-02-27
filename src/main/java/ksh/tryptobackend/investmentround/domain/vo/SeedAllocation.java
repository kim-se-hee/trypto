package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;

public record SeedAllocation(Long exchangeId, Long baseCurrencyCoinId, BigDecimal amount) {

    public static SeedAllocation create(Long exchangeId, Long baseCurrencyCoinId,
                                        BigDecimal amount, SeedAmountPolicy policy) {
        policy.validate(amount);
        return new SeedAllocation(exchangeId, baseCurrencyCoinId, amount);
    }
}
