package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record SnapshotSummary(Long userId, Long roundId, BigDecimal totalAssetKrw) {

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }
}
