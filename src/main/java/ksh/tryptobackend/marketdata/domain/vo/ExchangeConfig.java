package ksh.tryptobackend.marketdata.domain.vo;

import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;

import java.math.BigDecimal;

public record ExchangeConfig(
        String name,
        ExchangeMarketType marketType,
        String baseCurrencySymbol,
        BigDecimal feeRate
) {
}
