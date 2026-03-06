package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record ExchangeSnapshot(Long exchangeId, Long baseCurrencyCoinId, KrwConversionRate conversionRate) {

    public BigDecimal convertToKrw(BigDecimal amount) {
        return conversionRate.convert(amount);
    }
}
