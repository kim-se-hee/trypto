package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDateTime;

public record EligibleRound(Long userId, Long roundId, int tradeCount, LocalDateTime startedAt) {

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }
}
