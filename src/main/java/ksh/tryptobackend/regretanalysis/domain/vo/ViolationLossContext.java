package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public record ViolationLossContext(
    BigDecimal filledPrice,
    BigDecimal quantity,
    BigDecimal tradeAmount,
    BigDecimal currentPrice,
    List<SoldPortion> soldPortions
) {

    public record SoldPortion(BigDecimal price, BigDecimal quantity) {
    }
}
