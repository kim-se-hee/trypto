package ksh.tryptobackend.portfolio.domain.vo;

import java.util.List;

public class ActiveRounds {

    private final List<ActiveRound> values;

    public ActiveRounds(List<ActiveRound> values) {
        this.values = List.copyOf(values);
    }

    public List<Long> roundIds() {
        return values.stream()
            .map(ActiveRound::roundId)
            .toList();
    }

    public List<ActiveRound> values() {
        return values;
    }
}
