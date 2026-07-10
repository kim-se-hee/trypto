package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record Holding(Long coinId, Long exchangeId, BigDecimal assetRatio, BigDecimal profitRate) {}
