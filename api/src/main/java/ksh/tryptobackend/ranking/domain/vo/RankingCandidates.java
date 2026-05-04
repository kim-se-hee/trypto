package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import ksh.tryptobackend.ranking.domain.model.Ranking;

public class RankingCandidates {

    private final List<RankingCandidate> sorted;

    public RankingCandidates(List<RankingCandidate> candidates) {
        this.sorted = candidates.stream().sorted().toList();
    }

    public List<Ranking> toRankings(
            RankingPeriod period, LocalDate referenceDate, LocalDateTime createdAt) {
        return IntStream.range(0, sorted.size())
                .mapToObj(i -> toRanking(sorted.get(i), i + 1, period, referenceDate, createdAt))
                .toList();
    }

    private Ranking toRanking(
            RankingCandidate candidate,
            int rank,
            RankingPeriod period,
            LocalDate referenceDate,
            LocalDateTime createdAt) {
        return Ranking.create(
                candidate.userId(),
                candidate.roundId(),
                period,
                rank,
                candidate.profitRate(),
                candidate.tradeCount(),
                referenceDate,
                createdAt);
    }
}
