package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;

public record ExchangeConfig(
        String name,
        ExchangeMarketType marketType,
        String baseCurrencySymbol,
        BigDecimal feeRate) {}
