package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;

public interface MarketDataQueryPort {

    BigDecimal getChangeRate(Long exchangeCoinId);

    Long getBaseCurrencyCoinId(Long exchangeId);

    SeedFundingSpec getSeedFundingSpec(Long exchangeId);
}
