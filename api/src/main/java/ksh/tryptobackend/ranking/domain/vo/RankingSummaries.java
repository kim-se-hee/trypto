package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RankingSummaries {

    private static final int PROFIT_RATE_SCALE = 4;

    private final List<RankingSummary> summaries;
    private final boolean hasNext;

    private RankingSummaries(List<RankingSummary> summaries, boolean hasNext) {
        this.summaries = summaries;
        this.hasNext = hasNext;
    }

    public static RankingSummaries of(List<RankingSummary> summaries) {
        return new RankingSummaries(List.copyOf(summaries), false);
    }

    public static RankingSummaries fromOverflow(List<RankingSummary> fetched, int requestedSize) {
        boolean hasNext = fetched.size() > requestedSize;
        List<RankingSummary> trimmed =
                hasNext ? List.copyOf(fetched.subList(0, requestedSize)) : List.copyOf(fetched);
        return new RankingSummaries(trimmed, hasNext);
    }

    public Set<Long> userIds() {
        return summaries.stream().map(RankingSummary::userId).collect(Collectors.toSet());
    }

    public Integer nextCursorRank() {
        return hasNext ? summaries.getLast().rank() : null;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public List<RankingSummary> toList() {
        return summaries;
    }

    public RankingStats toStats() {
        return new RankingStats(summaries.size(), maxProfitRate(), avgProfitRate());
    }

    private BigDecimal maxProfitRate() {
        return summaries.stream()
                .map(RankingSummary::profitRate)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal avgProfitRate() {
        return summaries.stream()
                .map(RankingSummary::profitRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(summaries.size()),
                        PROFIT_RATE_SCALE,
                        RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RankingSummaries that = (RankingSummaries) o;
        return hasNext == that.hasNext && Objects.equals(summaries, that.summaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summaries, hasNext);
    }
}
