package ksh.tryptobackend.ranking.domain.vo;

import java.time.LocalDateTime;

public record ActiveRound(Long userId, Long roundId, LocalDateTime startedAt) {

    public EligibleRound toEligibleRound(int tradeCount) {
        return new EligibleRound(userId, roundId, tradeCount, startedAt);
    }
}
