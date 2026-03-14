package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.LivePrices;

import java.math.BigDecimal;
import java.util.Set;

public interface LivePriceQueryPort {

    BigDecimal getCurrentPrice(Long exchangeCoinId);

    LivePrices getCurrentPrices(Set<Long> exchangeCoinIds);
}
