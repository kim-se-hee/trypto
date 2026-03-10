package ksh.tryptobackend.portfolio.domain.vo;

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

    public List<PortfolioHolding> values() {
        return holdings;
    }
}
