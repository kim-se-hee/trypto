package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BtcDailyPrices {

    private final Map<LocalDate, BigDecimal> priceByDate;

    private BtcDailyPrices(Map<LocalDate, BigDecimal> priceByDate) {
        this.priceByDate = Map.copyOf(priceByDate);
    }

    public static BtcDailyPrices of(List<BtcDailyPrice> prices) {
        Map<LocalDate, BigDecimal> priceByDate = prices.stream()
            .collect(Collectors.toMap(BtcDailyPrice::date, BtcDailyPrice::closePrice));
        return new BtcDailyPrices(priceByDate);
    }

    public Map<LocalDate, BigDecimal> toMap() {
        return priceByDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BtcDailyPrices that = (BtcDailyPrices) o;
        return Objects.equals(priceByDate, that.priceByDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priceByDate);
    }
}
