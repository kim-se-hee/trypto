package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;

public interface LivePricePort {

    BigDecimal getCurrentPrice(Long exchangeCoinId);
}
