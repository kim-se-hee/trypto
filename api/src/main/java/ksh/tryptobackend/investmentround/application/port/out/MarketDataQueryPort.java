package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;

/** investmentround 가 marketdata 컨텍스트의 데이터를 조회하기 위한 포트. */
public interface MarketDataQueryPort {

    BigDecimal getChangeRate(Long exchangeCoinId);

    Long getBaseCurrencyCoinId(Long exchangeId);
}
