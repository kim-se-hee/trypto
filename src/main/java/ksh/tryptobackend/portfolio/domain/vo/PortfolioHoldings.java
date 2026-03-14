package ksh.tryptobackend.portfolio.domain.vo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PortfolioHoldings {

    private final List<PortfolioHolding> holdings;

    public PortfolioHoldings(List<PortfolioHolding> holdings) {
        this.holdings = List.copyOf(holdings);
    }

    public boolean isEmpty() {
        return holdings.isEmpty();
    }

    public Set<Long> coinIds() {
        return holdings.stream()
                .map(PortfolioHolding::coinId)
                .collect(Collectors.toSet());
    }

    public Set<Long> coinIdsIncluding(Long additionalCoinId) {
        Set<Long> allCoinIds = new HashSet<>(coinIds());
        allCoinIds.add(additionalCoinId);
        return Set.copyOf(allCoinIds);
    }

    public List<HoldingSnapshot> toHoldingSnapshots(CoinSnapshotMap coinSnapshotMap) {
        return holdings.stream()
                .map(holding -> {
                    CoinSnapshot coinSnapshot = coinSnapshotMap.getCoinSnapshot(holding.coinId());
                    return new HoldingSnapshot(
                            holding.coinId(),
                            coinSnapshot.symbol(),
                            coinSnapshot.name(),
                            holding.quantity(),
                            holding.avgBuyPrice(),
                            coinSnapshot.currentPrice()
                    );
                })
                .toList();
    }

    public List<PortfolioHolding> values() {
        return holdings;
    }
}
