package ksh.tryptobackend.marketdata.application.port.in;

import java.math.BigDecimal;

public interface GetPriceChangeRateUseCase {

    BigDecimal getChangeRate(Long exchangeCoinId);
}
