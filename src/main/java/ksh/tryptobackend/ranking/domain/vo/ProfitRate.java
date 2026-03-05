package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record ProfitRate(BigDecimal value) implements Comparable<ProfitRate> {

    private static final int RATE_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public ProfitRate {
        Objects.requireNonNull(value, "수익률은 null일 수 없습니다.");
    }

    public static ProfitRate of(BigDecimal value) {
        return new ProfitRate(value);
    }

    public static ProfitRate fromAssetChange(BigDecimal currentAsset, BigDecimal baseAsset) {
        if (baseAsset.compareTo(BigDecimal.ZERO) == 0) {
            return new ProfitRate(BigDecimal.ZERO);
        }
        BigDecimal rate = currentAsset.subtract(baseAsset)
            .divide(baseAsset, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(HUNDRED);
        return new ProfitRate(rate);
    }

    @Override
    public int compareTo(ProfitRate other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfitRate that)) return false;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
