package ksh.tryptobackend.ranking.domain.vo;

import java.util.Map;
import java.util.Objects;

public class RoundTradeCounts {

    private final Map<Long, Integer> countByRoundId;

    public RoundTradeCounts(Map<Long, Integer> countByRoundId) {
        this.countByRoundId = Map.copyOf(countByRoundId);
    }

    public int getCount(Long roundId) {
        return countByRoundId.getOrDefault(roundId, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoundTradeCounts that = (RoundTradeCounts) o;
        return Objects.equals(countByRoundId, that.countByRoundId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countByRoundId);
    }
}
