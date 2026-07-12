package ksh.tryptobackend.portfolio.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import ksh.tryptobackend.common.domain.vo.ProfitRate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SnapshotDetail {

    private static final int RATE_SCALE = 4;

    private final Long id;
    private final Long snapshotId;
    private final Long coinId;
    private final BigDecimal quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal profitRate;
    private final BigDecimal assetRatio;

    public static SnapshotDetail create(
            Long coinId, BigDecimal quantity, BigDecimal avgBuyPrice, BigDecimal currentPrice, BigDecimal totalAsset) {
        BigDecimal coinAsset = currentPrice.multiply(quantity);

        return SnapshotDetail.builder()
                .coinId(coinId)
                .quantity(quantity)
                .avgBuyPrice(avgBuyPrice)
                .currentPrice(currentPrice)
                .profitRate(
                        ProfitRate.fromAssetChange(currentPrice, avgBuyPrice).value())
                .assetRatio(calculateAssetRatio(coinAsset, totalAsset))
                .build();
    }

    private static BigDecimal calculateAssetRatio(BigDecimal coinAsset, BigDecimal totalAsset) {
        if (totalAsset.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return coinAsset.divide(totalAsset, RATE_SCALE, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }
}
