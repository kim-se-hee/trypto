package ksh.tryptobackend.ranking.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface LivePricePort {

    BigDecimal getCurrentPrice(Long exchangeCoinId);

    default Map<Long, BigDecimal> getCurrentPrices(List<Long> exchangeCoinIds) {
        return exchangeCoinIds.stream()
            .collect(Collectors.toMap(id -> id, this::getCurrentPrice));
    }
}
