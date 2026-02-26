package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;

public interface PriceChangeRatePort {

    BigDecimal getChangeRate(Long exchangeCoinId);
}
