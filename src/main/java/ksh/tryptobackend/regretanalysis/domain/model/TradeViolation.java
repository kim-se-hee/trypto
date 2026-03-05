package ksh.tryptobackend.regretanalysis.domain.model;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.strategy.ViolationLossStrategy;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class TradeViolation {

    private final Long orderId;
    private final Long ruleId;
    private final RuleType ruleType;
    private final TradeSide side;
    private final BigDecimal filledPrice;
    private final BigDecimal quantity;
    private final BigDecimal amount;
    private final Long exchangeCoinId;
    private final LocalDateTime violatedAt;
    private final List<SoldPortion> soldPortions;
    private final ViolationLossStrategy lossStrategy;

    public static TradeViolation create(Long orderId, Long ruleId, RuleType ruleType,
                                         TradeSide side, BigDecimal filledPrice,
                                         BigDecimal quantity, BigDecimal amount,
                                         Long exchangeCoinId, LocalDateTime violatedAt,
                                         List<SoldPortion> soldPortions) {
        return TradeViolation.builder()
            .orderId(orderId)
            .ruleId(ruleId)
            .ruleType(ruleType)
            .side(side)
            .filledPrice(filledPrice)
            .quantity(quantity)
            .amount(amount)
            .exchangeCoinId(exchangeCoinId)
            .violatedAt(violatedAt)
            .soldPortions(List.copyOf(soldPortions))
            .lossStrategy(ViolationLossStrategy.resolve(ruleType, side == TradeSide.BUY))
            .build();
    }

    public BigDecimal calculateLoss(BigDecimal currentPrice) {
        ViolationLossContext context = new ViolationLossContext(
            filledPrice, quantity, amount, currentPrice, soldPortions);
        return lossStrategy.calculateLoss(context);
    }
}
