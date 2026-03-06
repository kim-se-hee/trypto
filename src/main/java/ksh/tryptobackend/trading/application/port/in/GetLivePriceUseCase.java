package ksh.tryptobackend.trading.application.port.in;

import java.math.BigDecimal;

public interface GetLivePriceUseCase {

    BigDecimal getCurrentPrice(Long exchangeCoinId);
}
