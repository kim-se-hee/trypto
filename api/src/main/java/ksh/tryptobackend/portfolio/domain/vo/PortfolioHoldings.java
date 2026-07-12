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

    public Set<Long> coinIdsIncluding(Long additionalCoinId) {
        Set<Long> coinIds =
                holdings.stream().map(PortfolioHolding::coinId).collect(Collectors.toCollection(HashSet::new));
        coinIds.add(additionalCoinId);
        return Set.copyOf(coinIds);
    }

    public List<HoldingSnapshot> toHoldingSnapshots(CoinMetadataMap coinMetadata) {
        return holdings.stream()
                .filter(holding -> coinMetadata.hasMetadata(holding.coinId()))
                .map(holding -> holding.toSnapshot(coinMetadata.getMetadata(holding.coinId())))
                .toList();
    }
}
