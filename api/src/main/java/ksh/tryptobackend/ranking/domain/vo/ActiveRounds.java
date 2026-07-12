package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ActiveRounds {

    private final List<ActiveRound> rounds;

    public ActiveRounds(List<ActiveRound> rounds) {
        this.rounds = List.copyOf(rounds);
    }

    public List<Long> roundIds() {
        return rounds.stream().map(ActiveRound::roundId).toList();
    }

    public EligibleRounds toEligibleRounds(RoundTradeCounts tradeCounts, LocalDate snapshotDate) {
        List<EligibleRound> eligible = rounds.stream()
                .map(round -> round.toEligibleRound(tradeCounts.getCount(round.roundId())))
                .toList();
        return EligibleRounds.of(eligible, snapshotDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveRounds that = (ActiveRounds) o;
        return Objects.equals(rounds, that.rounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rounds);
    }
}
