package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDateTime;

public record ActiveRoundInfo(Long roundId, Long userId, LocalDateTime startedAt) {

    public boolean isStartedBefore(LocalDateTime cutoff) {
        return startedAt.isBefore(cutoff);
    }

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }
}
