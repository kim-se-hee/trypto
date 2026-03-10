package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;

public record PortfolioHolding(
        Long coinId,
        BigDecimal avgBuyPrice,
        BigDecimal quantity
) {
}
