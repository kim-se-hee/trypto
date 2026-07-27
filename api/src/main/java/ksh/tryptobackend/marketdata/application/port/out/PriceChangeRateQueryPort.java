package ksh.tryptobackend.marketdata.application.port.out;

import java.math.BigDecimal;

public interface PriceChangeRateQueryPort {

    /**
     * 24시간 변동률을 퍼센트 단위로 반환한다. +1.23% 는 {@code 1.23} 이다.
     * 저장소는 비율(1% = 0.01)로 적재하므로 구현체가 환산 책임을 진다.
     */
    BigDecimal getChangeRate(Long exchangeCoinId);
}
