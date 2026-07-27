package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;

public record AnalysisExchange(Long exchangeId, String name, String currency) {

    private static final String DOMESTIC_CURRENCY = "KRW";

    public boolean isDomestic() {
        return DOMESTIC_CURRENCY.equals(currency);
    }

    public BigDecimal toKrw(BigDecimal amount) {
        return conversionRate().convert(amount);
    }

    private KrwConversionRate conversionRate() {
        return isDomestic() ? KrwConversionRate.DOMESTIC : KrwConversionRate.OVERSEAS;
    }
}
