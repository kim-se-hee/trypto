package ksh.tryptobackend.marketdata.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;

public interface ExchangeCommandPort {

    Exchange save(
            String name,
            ExchangeMarketType marketType,
            Long baseCurrencyCoinId,
            BigDecimal feeRate);
}
