package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.EligibleRoundQueryPort;
import ksh.tryptobackend.ranking.domain.vo.EligibleRound;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockEligibleRoundQueryAdapter implements EligibleRoundQueryPort {

    private final List<EligibleRound> eligibleRounds = new ArrayList<>();

    @Override
    public List<EligibleRound> findAll() {
        return List.copyOf(eligibleRounds);
    }

    public void addEligibleRound(Long userId, Long roundId, int tradeCount, LocalDateTime startedAt) {
        eligibleRounds.add(new EligibleRound(userId, roundId, tradeCount, startedAt));
    }

    public void clear() {
        eligibleRounds.clear();
    }
}
