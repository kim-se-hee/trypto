package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class SnapshotSummaries {

    private final Map<RoundKey, BigDecimal> totalAssetMap;

    public SnapshotSummaries(Map<RoundKey, BigDecimal> totalAssetMap) {
        this.totalAssetMap = totalAssetMap;
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
