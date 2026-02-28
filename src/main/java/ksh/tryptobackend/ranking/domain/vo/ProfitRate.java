package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public record ProfitRate(BigDecimal value) {

    public ProfitRate {
        Objects.requireNonNull(value, "수익률은 null일 수 없습니다.");
    }

    public static ProfitRate of(BigDecimal value) {
        return new ProfitRate(value);
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
