package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;

public record PortfolioHolding(Long coinId, BigDecimal avgBuyPrice, BigDecimal quantity, BigDecimal currentPrice) {

    public HoldingSnapshot toSnapshot(CoinMetadata metadata) {
        return new HoldingSnapshot(coinId, metadata.symbol(), metadata.name(), quantity, avgBuyPrice, currentPrice);
    }
}
