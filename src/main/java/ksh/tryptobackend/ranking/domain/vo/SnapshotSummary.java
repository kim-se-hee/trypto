package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SnapshotSummary(
    Long userId,
    Long roundId,
    BigDecimal totalAssetKrw,
    BigDecimal totalInvestmentKrw
) {

    public RoundKey roundKey() {
        return new RoundKey(userId, roundId);
    }

    public static Map<RoundKey, BigDecimal> toTotalAssetMap(List<SnapshotSummary> summaries) {
        return summaries.stream()
            .collect(Collectors.toMap(SnapshotSummary::roundKey, SnapshotSummary::totalAssetKrw));
    }
}
