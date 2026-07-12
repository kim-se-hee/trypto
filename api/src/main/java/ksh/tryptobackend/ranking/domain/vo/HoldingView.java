package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record HoldingView(String coinSymbol, String exchangeName, BigDecimal assetRatio, BigDecimal profitRate) {}
