package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BtcDailyPrices {

    private final Map<LocalDate, BigDecimal> priceByDate;

    private BtcDailyPrices(Map<LocalDate, BigDecimal> priceByDate) {
        this.priceByDate = Map.copyOf(priceByDate);
    }

    public static BtcDailyPrices of(List<BtcDailyPrice> prices) {
        Map<LocalDate, BigDecimal> priceByDate =
                prices.stream().collect(Collectors.toMap(BtcDailyPrice::date, BtcDailyPrice::closePrice));
        return new BtcDailyPrices(priceByDate);
    }

    public Optional<BigDecimal> findClosingPriceAt(LocalDate date) {
        return Optional.ofNullable(priceByDate.get(date));
    }

    /** 자금이 들어온 날에 종가가 없으면(주말·수집 누락) 그 이전 가장 가까운 종가로 산다. */
    public Optional<BigDecimal> findLatestClosingPriceUntil(LocalDate date) {
        return priceByDate.keySet().stream()
                .filter(priced -> !priced.isAfter(date))
                .max(LocalDate::compareTo)
                .map(priceByDate::get);
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
