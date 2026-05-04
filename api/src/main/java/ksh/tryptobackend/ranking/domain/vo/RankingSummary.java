package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record RankingSummary(int rank, Long userId, BigDecimal profitRate, int tradeCount) {}
