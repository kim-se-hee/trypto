package ksh.tryptobackend.trading.application.port.out;

import java.util.Optional;

public interface ExchangePort {

    Optional<ExchangeData> findById(Long exchangeId);

    record ExchangeData(
        Long exchangeId,
        java.math.BigDecimal feeRate,
        Long baseCurrencyCoinId,
        String baseCurrencySymbol
    ) {
    }
}
