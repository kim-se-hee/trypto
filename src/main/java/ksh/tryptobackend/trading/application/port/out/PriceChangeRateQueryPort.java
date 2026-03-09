package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;

public interface PriceChangeRateQueryPort {

    BigDecimal getChangeRate(Long exchangeCoinId);
}
