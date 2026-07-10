package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;

/** investmentround 가 marketdata 컨텍스트의 데이터를 조회하기 위한 포트. */
public interface MarketDataQueryPort {

    BigDecimal getChangeRate(Long exchangeCoinId);

    Long getBaseCurrencyCoinId(Long exchangeId);

    SeedFundingSpec getSeedFundingSpec(Long exchangeId);
}
