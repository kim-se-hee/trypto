package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.domain.vo.KrwConversionRate;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;

public interface MarketDataQueryPort {

    /** 24시간 변동률을 퍼센트 단위로 반환한다. +1.23% 는 {@code 1.23} 이다. */
    BigDecimal getChangeRate(Long exchangeCoinId);

    Long getBaseCurrencyCoinId(Long exchangeId);

    SeedFundingSpec getSeedFundingSpec(Long exchangeId);

    /** 거래소 기축통화 → 원화 환산 시세. 원화 거래소는 항등 환산이다. */
    KrwConversionRate getKrwConversionRate(Long exchangeId);

    Long getCashInflowExchangeId();
}
