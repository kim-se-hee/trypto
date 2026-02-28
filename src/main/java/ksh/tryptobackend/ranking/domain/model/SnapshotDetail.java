package ksh.tryptobackend.ranking.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SnapshotDetail {

    private final Long id;
    private final Long snapshotId;
    private final Long coinId;
    private final Long exchangeId;
    private final BigDecimal quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal profitRate;
    private final BigDecimal assetRatio;
}
