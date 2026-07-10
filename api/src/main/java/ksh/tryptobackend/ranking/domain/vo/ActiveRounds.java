package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDate;
import java.util.List;

public class ActiveRounds {

    private final List<ActiveRound> rounds;

    public ActiveRounds(List<ActiveRound> rounds) {
        this.rounds = List.copyOf(rounds);
    }

    public List<Long> roundIds() {
        return rounds.stream().map(ActiveRound::roundId).toList();
    }

    public EligibleRounds toEligibleRounds(RoundTradeCounts tradeCounts, LocalDate snapshotDate) {
        List<EligibleRound> eligibleRounds =
                rounds.stream()
                        .map(round -> round.toEligibleRound(tradeCounts.getCount(round.roundId())))
                        .toList();
        return EligibleRounds.of(eligibleRounds, snapshotDate);
    }
}
