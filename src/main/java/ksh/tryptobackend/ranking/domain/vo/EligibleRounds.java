package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EligibleRounds {

    private static final int ELIGIBILITY_HOURS = 24;

    private final List<EligibleRound> rounds;

    private EligibleRounds(List<EligibleRound> rounds) {
        this.rounds = rounds;
    }

    public static EligibleRounds of(List<EligibleRound> candidates, LocalDate snapshotDate) {
        LocalDateTime cutoff = snapshotDate.atStartOfDay().minusHours(ELIGIBILITY_HOURS);

        List<EligibleRound> eligible = candidates.stream()
            .filter(round -> round.startedAt().isBefore(cutoff))
            .filter(round -> round.tradeCount() > 0)
            .toList();

        return new EligibleRounds(eligible);
    }

    public boolean isEmpty() {
        return rounds.isEmpty();
    }

    public RankingCandidates toCandidates(SnapshotSummaries today, SnapshotSummaries comparison) {
        List<RankingCandidate> candidates = new ArrayList<>();

        for (EligibleRound round : rounds) {
            today.calculateProfitRate(round.roundKey(), comparison)
                .ifPresent(profitRate -> candidates.add(new RankingCandidate(
                    round.userId(), round.roundId(), profitRate,
                    round.tradeCount(), round.startedAt()
                )));
        }

        return new RankingCandidates(candidates);
    }
}
