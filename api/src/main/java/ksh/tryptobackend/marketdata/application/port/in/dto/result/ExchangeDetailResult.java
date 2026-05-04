package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;

public record ExchangeDetailResult(
        String name, Long baseCurrencyCoinId, boolean domestic, BigDecimal feeRate) {}
