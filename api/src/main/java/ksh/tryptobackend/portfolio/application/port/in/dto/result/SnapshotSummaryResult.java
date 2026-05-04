package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;

public record SnapshotSummaryResult(
        Long userId, Long roundId, BigDecimal totalAssetKrw, BigDecimal totalInvestmentKrw) {}
