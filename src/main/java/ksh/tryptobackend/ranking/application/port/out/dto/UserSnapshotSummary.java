package ksh.tryptobackend.ranking.application.port.out.dto;

import ksh.tryptobackend.ranking.domain.vo.RoundKey;

import java.math.BigDecimal;

public record UserSnapshotSummary(
    Long userId,
    Long roundId,
    BigDecimal totalAssetKrw,
    BigDecimal totalInvestmentKrw
) {

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }
}
