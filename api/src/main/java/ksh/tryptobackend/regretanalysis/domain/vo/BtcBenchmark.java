package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BtcBenchmark {

    private static final int QUANTITY_SCALE = 8;
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    public record DailyValue(LocalDate date, BigDecimal assetValue) {}

    private final List<DailyValue> entries;
    private final Map<LocalDate, DailyValue> entryByDate;

    private BtcBenchmark(List<DailyValue> entries) {
        this.entries = entries;
        this.entryByDate = entries.stream().collect(Collectors.toMap(DailyValue::date, Function.identity()));
    }

    public static BtcBenchmark calculate(CapitalInflows inflows, BtcDailyPrices btcDailyPrices, List<LocalDate> dates) {
        List<DailyValue> result = new ArrayList<>();
        BigDecimal holdingQuantity = BigDecimal.ZERO;
        int inflowIndex = 0;
        List<CapitalInflows.CapitalInflow> pendingInflows = inflows.values();

        for (LocalDate date : dates) {
            while (inflowIndex < pendingInflows.size()
                    && !pendingInflows.get(inflowIndex).date().isAfter(date)) {
                holdingQuantity = holdingQuantity.add(buyQuantity(pendingInflows.get(inflowIndex), btcDailyPrices));
                inflowIndex++;
            }
            result.add(new DailyValue(date, evaluate(holdingQuantity, date, btcDailyPrices)));
        }
        return new BtcBenchmark(result);
    }

    private static BigDecimal buyQuantity(CapitalInflows.CapitalInflow inflow, BtcDailyPrices btcDailyPrices) {
        return btcDailyPrices
                .findLatestClosingPriceUntil(inflow.date())
                .filter(price -> price.signum() > 0)
                .map(price -> inflow.amount().divide(price, QUANTITY_SCALE, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal evaluate(BigDecimal quantity, LocalDate date, BtcDailyPrices btcDailyPrices) {
        return btcDailyPrices
                .findClosingPriceAt(date)
                .map(price -> quantity.multiply(price, MATH_CONTEXT))
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getAssetValueAt(LocalDate date) {
        DailyValue entry = entryByDate.get(date);
        return entry != null ? entry.assetValue() : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BtcBenchmark that)) return false;
        if (entries.size() != that.entries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            DailyValue a = entries.get(i);
            DailyValue b = that.entries.get(i);
            if (!a.date().equals(b.date()) || a.assetValue().compareTo(b.assetValue()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(entries.size());
        for (DailyValue entry : entries) {
            result = 31 * result + Objects.hash(entry.date(), entry.assetValue().stripTrailingZeros());
        }
        return result;
    }
}
