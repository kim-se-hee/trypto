package ksh.tryptobackend.batch.ranking;

import ksh.tryptobackend.ranking.application.port.out.dto.RoundKey;
import ksh.tryptobackend.ranking.application.port.out.dto.UserSnapshotSummary;
import ksh.tryptobackend.ranking.domain.vo.ProfitRate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class SnapshotSummaries {

    private final Map<RoundKey, UserSnapshotSummary> summaryMap;

    SnapshotSummaries(List<UserSnapshotSummary> summaries) {
        this.summaryMap = summaries.stream()
            .collect(Collectors.toMap(UserSnapshotSummary::roundKey, s -> s));
    }

    Optional<ProfitRate> calculateProfitRate(RoundKey roundKey, SnapshotSummaries base) {
        UserSnapshotSummary current = summaryMap.get(roundKey);
        UserSnapshotSummary baseline = base.summaryMap.get(roundKey);

        if (current == null || baseline == null) {
            return Optional.empty();
        }

        return Optional.of(ProfitRate.fromAssetChange(current.totalAssetKrw(), baseline.totalAssetKrw()));
    }
}
