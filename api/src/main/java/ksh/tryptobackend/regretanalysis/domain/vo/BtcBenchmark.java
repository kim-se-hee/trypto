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

    private static final int PRICE_SCALE = 8;
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    public record DailyValue(LocalDate date, BigDecimal assetValue) {}

    private final List<DailyValue> entries;
    private final Map<LocalDate, DailyValue> entryByDate;

    private BtcBenchmark(List<DailyValue> entries) {
        this.entries = entries;
        this.entryByDate = entries.stream().collect(Collectors.toMap(DailyValue::date, Function.identity()));
    }

    public static BtcBenchmark calculate(
            BigDecimal seedMoney,
            Map<LocalDate, BigDecimal> btcPriceByDate,
            List<LocalDate> snapshotDates,
            LocalDate startDate) {
        BigDecimal btcPriceAtStart = btcPriceByDate.get(startDate);
        if (btcPriceAtStart == null || btcPriceAtStart.compareTo(BigDecimal.ZERO) == 0) {
            return new BtcBenchmark(List.of());
        }

        BigDecimal btcQuantity = seedMoney.divide(btcPriceAtStart, PRICE_SCALE, RoundingMode.HALF_UP);

        List<DailyValue> result = new ArrayList<>();
        for (LocalDate date : snapshotDates) {
            BigDecimal dailyPrice = btcPriceByDate.get(date);
            if (dailyPrice == null) {
                result.add(new DailyValue(date, BigDecimal.ZERO));
            } else {
                result.add(new DailyValue(date, btcQuantity.multiply(dailyPrice, MATH_CONTEXT)));
            }
        }
        return new BtcBenchmark(result);
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
