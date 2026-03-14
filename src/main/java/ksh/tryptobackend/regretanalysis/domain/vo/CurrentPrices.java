package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public class CurrentPrices {

    private final Map<Long, BigDecimal> priceByExchangeCoinId;

    public CurrentPrices(Map<Long, BigDecimal> priceByExchangeCoinId) {
        this.priceByExchangeCoinId = Map.copyOf(priceByExchangeCoinId);
    }

    public BigDecimal getPrice(Long exchangeCoinId) {
        return priceByExchangeCoinId.get(exchangeCoinId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentPrices that = (CurrentPrices) o;
        return Objects.equals(priceByExchangeCoinId, that.priceByExchangeCoinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priceByExchangeCoinId);
    }
}
