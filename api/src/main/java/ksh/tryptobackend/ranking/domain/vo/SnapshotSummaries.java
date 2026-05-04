package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.domain.vo.ProfitRate;

public class SnapshotSummaries {

    private final Map<RoundKey, BigDecimal> totalAssetMap;

    public SnapshotSummaries(List<SnapshotSummary> summaries) {
        this.totalAssetMap =
                summaries.stream()
                        .collect(
                                Collectors.toMap(
                                        SnapshotSummary::roundKey, SnapshotSummary::totalAssetKrw));
    }

    public Optional<ProfitRate> calculateProfitRate(RoundKey key, SnapshotSummaries previous) {
        BigDecimal current = totalAssetMap.get(key);
        BigDecimal previousAsset = previous.totalAssetMap.get(key);

        if (current == null || previousAsset == null) {
            return Optional.empty();
        }

        return Optional.of(ProfitRate.fromAssetChange(current, previousAsset));
    }
}
