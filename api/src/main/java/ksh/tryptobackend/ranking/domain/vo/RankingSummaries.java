package ksh.tryptobackend.ranking.domain.vo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RankingSummaries {

    private final List<RankingSummary> summaries;
    private final boolean hasNext;

    private RankingSummaries(List<RankingSummary> summaries, boolean hasNext) {
        this.summaries = summaries;
        this.hasNext = hasNext;
    }

    public static RankingSummaries fromOverflow(List<RankingSummary> fetched, int requestedSize) {
        boolean hasNext = fetched.size() > requestedSize;
        List<RankingSummary> trimmed = hasNext ? List.copyOf(fetched.subList(0, requestedSize)) : List.copyOf(fetched);
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
