package ksh.tryptobackend.ranking.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PortfolioSnapshot {

    private final Long id;
    private final Long userId;
    private final Long roundId;
    private final BigDecimal totalAssetKrw;
    private final BigDecimal totalProfitKrw;
    private final BigDecimal totalProfitRate;
    private final LocalDateTime snapshotDate;
}
