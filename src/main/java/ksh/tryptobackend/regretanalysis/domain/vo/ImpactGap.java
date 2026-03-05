package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class ImpactGap {

    private static final int RATE_SCALE = 4;

    private final BigDecimal value;

    private ImpactGap(BigDecimal value) {
        this.value = value;
    }

    public static ImpactGap of(BigDecimal value) {
        return new ImpactGap(value);
    }

    public static ImpactGap calculate(BigDecimal totalLoss, BigDecimal totalInvestment) {
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return ImpactGap.of(BigDecimal.ZERO);
        }
        BigDecimal gap = totalLoss
            .divide(totalInvestment, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return new ImpactGap(gap);
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImpactGap that)) return false;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value + "%p";
    }
}
