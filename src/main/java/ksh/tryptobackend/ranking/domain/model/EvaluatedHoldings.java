package ksh.tryptobackend.ranking.domain.model;

import java.math.BigDecimal;
import java.util.List;

public class EvaluatedHoldings {

    private final List<EvaluatedHolding> holdings;

    public EvaluatedHoldings(List<EvaluatedHolding> holdings) {
        this.holdings = holdings;
    }

    public BigDecimal totalEvaluatedAmount() {
        return holdings.stream()
            .map(EvaluatedHolding::getEvaluatedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SnapshotDetail> toSnapshotDetails(BigDecimal totalAsset) {
        return holdings.stream()
            .map(h -> h.toSnapshotDetail(totalAsset))
            .toList();
    }
}
