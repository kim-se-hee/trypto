package ksh.tryptobackend.marketdata.application.port.out;

import java.math.BigDecimal;
import java.util.Set;
import ksh.tryptobackend.marketdata.domain.vo.LivePrices;

public interface LivePriceQueryPort {

    BigDecimal getCurrentPrice(Long exchangeCoinId);

    LivePrices getCurrentPrices(Set<Long> exchangeCoinIds);
}
