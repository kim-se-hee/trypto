package ksh.tryptobackend.ranking.domain.vo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Holdings {

    private final List<Holding> holdings;

    public Holdings(List<Holding> holdings) {
        this.holdings = List.copyOf(holdings);
    }

    public Set<Long> coinIds() {
        return holdings.stream().map(Holding::coinId).collect(Collectors.toSet());
    }

    public Set<Long> exchangeIds() {
        return holdings.stream().map(Holding::exchangeId).collect(Collectors.toSet());
    }

    public List<HoldingView> describe(CoinSymbols coinSymbols, ExchangeNames exchangeNames) {
        return holdings.stream()
                .map(holding -> new HoldingView(
                        coinSymbols.getSymbol(holding.coinId()),
                        exchangeNames.getName(holding.exchangeId()),
                        holding.assetRatio(),
                        holding.profitRate()))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holdings that = (Holdings) o;
        return Objects.equals(holdings, that.holdings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holdings);
    }
}
