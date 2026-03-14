package ksh.tryptobackend.marketdata.application.port.out;

import java.math.BigDecimal;

public interface PriceChangeRateQueryPort {

    BigDecimal getChangeRate(Long exchangeCoinId);
}
