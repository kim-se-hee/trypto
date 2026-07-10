package ksh.tryptobackend.portfolio.domain.model;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadataMap;
import ksh.tryptobackend.portfolio.domain.vo.HoldingSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;

public class Portfolio {

    private final Long exchangeId;
    private final Long baseCurrencyCoinId;
    private final BigDecimal baseCurrencyBalance;
    private final PortfolioHoldings holdings;
    private final CoinMetadataMap coinMetadata;

    public Portfolio(
            Long exchangeId,
            Long baseCurrencyCoinId,
            BigDecimal baseCurrencyBalance,
            PortfolioHoldings holdings,
            CoinMetadataMap coinMetadata) {
        this.exchangeId = exchangeId;
        this.baseCurrencyCoinId = baseCurrencyCoinId;
        this.baseCurrencyBalance = baseCurrencyBalance;
        this.holdings = holdings;
        this.coinMetadata = coinMetadata;
    }

    public Long exchangeId() {
        return exchangeId;
    }

    public BigDecimal baseCurrencyBalance() {
        return baseCurrencyBalance;
    }

    public String baseCurrencySymbol() {
        return coinMetadata.getSymbol(baseCurrencyCoinId);
    }

    public List<HoldingSnapshot> holdingSnapshots() {
        return holdings.toHoldingSnapshots(coinMetadata);
    }
}
