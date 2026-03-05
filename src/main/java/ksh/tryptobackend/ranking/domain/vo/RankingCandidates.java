package ksh.tryptobackend.ranking.domain.vo;

import ksh.tryptobackend.ranking.domain.model.Ranking;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

public class RankingCandidates {

    private final List<RankingCandidate> sorted;

    public RankingCandidates(List<RankingCandidate> candidates) {
        this.sorted = candidates.stream().sorted().toList();
    }

    public List<Ranking> toRankings(RankingPeriod period, LocalDate referenceDate) {
        return IntStream.range(0, sorted.size())
            .mapToObj(i -> toRanking(sorted.get(i), i + 1, period, referenceDate))
            .toList();
    }

    private Ranking toRanking(RankingCandidate candidate, int rank,
                              RankingPeriod period, LocalDate referenceDate) {
        return Ranking.create(
            candidate.userId(), candidate.roundId(), period,
            rank, candidate.profitRate(), candidate.tradeCount(),
            referenceDate
        );
    }
}
