package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.domain.vo.ActiveRoundInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockActiveRoundQueryAdapter implements ActiveRoundQueryPort {

    private final List<ActiveRoundInfo> activeRounds = new ArrayList<>();

    @Override
    public List<ActiveRoundInfo> findAllActiveRounds() {
        return List.copyOf(activeRounds);
    }

    public void addActiveRound(Long roundId, Long userId, LocalDateTime startedAt) {
        activeRounds.add(new ActiveRoundInfo(roundId, userId, startedAt));
    }

    public void clear() {
        activeRounds.clear();
    }
}
