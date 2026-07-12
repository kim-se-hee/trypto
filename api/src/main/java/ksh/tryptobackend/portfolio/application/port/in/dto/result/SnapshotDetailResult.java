package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;

public record SnapshotDetailResult(Long coinId, Long exchangeId, BigDecimal assetRatio, BigDecimal profitRate) {}
