package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDateTime;
import java.util.Comparator;

public record RankingCandidate(
    Long userId,
    Long roundId,
    ProfitRate profitRate,
    int tradeCount,
    LocalDateTime startedAt
) implements Comparable<RankingCandidate> {

    private static final Comparator<RankingCandidate> COMPARATOR =
        Comparator.comparing(RankingCandidate::profitRate).reversed()
            .thenComparing(RankingCandidate::tradeCount)
            .thenComparing(RankingCandidate::startedAt)
            .thenComparing(RankingCandidate::userId);

    @Override
    public int compareTo(RankingCandidate other) {
        return COMPARATOR.compare(this, other);
    }
}
