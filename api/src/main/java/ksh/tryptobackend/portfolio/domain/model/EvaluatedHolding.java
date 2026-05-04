package ksh.tryptobackend.portfolio.domain.model;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class EvaluatedHolding {

    private final Long coinId;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal quantity;
    private final BigDecimal currentPrice;
    private final BigDecimal evaluatedAmount;

    public static EvaluatedHolding create(
            Long coinId, BigDecimal avgBuyPrice, BigDecimal quantity, BigDecimal currentPrice) {
        return EvaluatedHolding.builder()
                .coinId(coinId)
                .avgBuyPrice(avgBuyPrice)
                .quantity(quantity)
                .currentPrice(currentPrice)
                .evaluatedAmount(currentPrice.multiply(quantity))
                .build();
    }

    public SnapshotDetail toSnapshotDetail(BigDecimal totalAsset) {
        return SnapshotDetail.create(coinId, quantity, avgBuyPrice, currentPrice, totalAsset);
    }
}
