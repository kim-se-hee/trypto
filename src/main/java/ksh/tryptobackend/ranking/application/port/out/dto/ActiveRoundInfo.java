package ksh.tryptobackend.ranking.application.port.out.dto;

import ksh.tryptobackend.ranking.domain.vo.RoundKey;

import java.time.LocalDateTime;

public record ActiveRoundInfo(Long roundId, Long userId, LocalDateTime startedAt) {

    public boolean isStartedBefore(LocalDateTime cutoff) {
        return startedAt.isBefore(cutoff);
    }

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }
}
