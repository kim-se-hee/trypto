package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Getter
@Builder
public class Holding {

    private static final int PRICE_SCALE = 8;

    private final Long id;
    private final Long walletId;
    private final Long coinId;
    private BigDecimal avgBuyPrice;
    private BigDecimal totalQuantity;
    private BigDecimal totalBuyAmount;
    private int averagingDownCount;

    public static Holding empty(Long walletId, Long coinId) {
        return Holding.builder()
            .walletId(walletId)
            .coinId(coinId)
            .avgBuyPrice(BigDecimal.ZERO)
            .totalQuantity(BigDecimal.ZERO)
            .totalBuyAmount(BigDecimal.ZERO)
            .averagingDownCount(0)
            .build();
    }

    public void applyOrder(Side side, BigDecimal filledPrice, BigDecimal quantity, BigDecimal currentPrice) {
        if (side == Side.BUY) {
            applyBuy(filledPrice, quantity, currentPrice);
        } else {
            applySell(quantity);
        }
    }

    public void replayFrom(List<FilledOrder> filledOrders) {
        reset();
        this.averagingDownCount = 0;
        for (FilledOrder f : filledOrders) {
            applyOrder(f.side(), f.filledPrice(), f.quantity(), f.filledPrice());
        }
    }

    public void applyBuy(BigDecimal filledPrice, BigDecimal filledQuantity, BigDecimal currentPrice) {
        if (isHolding() && isAtLoss(currentPrice)) {
            this.averagingDownCount++;
        }
        BigDecimal buyAmount = filledPrice.multiply(filledQuantity);
        this.totalBuyAmount = totalBuyAmount.add(buyAmount);
        this.totalQuantity = totalQuantity.add(filledQuantity);
        this.avgBuyPrice = totalBuyAmount.divide(totalQuantity, PRICE_SCALE, RoundingMode.FLOOR);
    }

    public void applySell(BigDecimal filledQuantity) {
        this.totalQuantity = totalQuantity.subtract(filledQuantity);
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            reset();
        }
    }

    public boolean isHolding() {
        return totalQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isAtLoss(BigDecimal currentPrice) {
        return isHolding() && avgBuyPrice.compareTo(currentPrice) > 0;
    }

    private void reset() {
        this.avgBuyPrice = BigDecimal.ZERO;
        this.totalQuantity = BigDecimal.ZERO;
        this.totalBuyAmount = BigDecimal.ZERO;
    }
}
