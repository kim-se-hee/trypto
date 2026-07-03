package ksh.tryptobackend.trading.domain.model;

import java.util.List;
import ksh.tryptobackend.trading.domain.vo.ExecutedFill;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.vo.Holding;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Position {

    private final Long id;
    private final Long walletId;
    private final Long coinId;

    private Holding holding;
    private int averagingDownCount;

    public static Position empty(Long walletId, Long coinId) {
        return Position.builder()
                .walletId(walletId)
                .coinId(coinId)
                .holding(Holding.empty())
                .averagingDownCount(0)
                .build();
    }

    public void applyFill(ExecutedFill fill, Price currentPrice) {
        if (fill.side() == Side.BUY) {
            applyBuy(fill, currentPrice);
        } else {
            applySell(fill);
        }
    }

    public void replayFrom(List<FilledOrder> filledOrders) {
        this.holding = Holding.empty();
        this.averagingDownCount = 0;
        for (FilledOrder filled : filledOrders) {
            Price filledPrice = Price.of(filled.filledPrice());
            ExecutedFill fill =
                    ExecutedFill.of(filled.side(), filledPrice, Quantity.of(filled.quantity()));
            applyFill(fill, filledPrice);
        }
    }

    public boolean isHolding() {
        return holding.isHolding();
    }

    public boolean isAtLoss(Price currentPrice) {
        return holding.isAtLoss(currentPrice);
    }

    private void applyBuy(ExecutedFill fill, Price currentPrice) {
        if (holding.isAtLoss(currentPrice)) {
            this.averagingDownCount++;
        }
        this.holding = holding.addBuy(fill.filledPrice(), fill.quantity());
    }

    private void applySell(ExecutedFill fill) {
        this.holding = holding.reduce(fill.quantity());
    }
}
