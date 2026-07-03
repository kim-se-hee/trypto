package ksh.tryptobackend.trading.domain.vo;

public record Holding(Price avgBuyPrice, Quantity totalQuantity, Money totalBuyAmount) {

    public static Holding empty() {
        return new Holding(Price.zero(), Quantity.zero(), Money.zero());
    }

    public Holding addBuy(Price filledPrice, Quantity filledQuantity) {
        Money amount = totalBuyAmount.plus(filledPrice.times(filledQuantity));
        Quantity quantity = totalQuantity.plus(filledQuantity);
        return new Holding(amount.dividedBy(quantity), quantity, amount);
    }

    public Holding reduce(Quantity filledQuantity) {
        Quantity quantity = totalQuantity.minus(filledQuantity);
        if (!quantity.isPositive()) {
            return empty();
        }
        Money amount = totalBuyAmount.minus(avgBuyPrice.times(filledQuantity));
        return new Holding(avgBuyPrice, quantity, amount);
    }

    public boolean isHolding() {
        return totalQuantity.isPositive();
    }

    public boolean isAtLoss(Price currentPrice) {
        return isHolding() && avgBuyPrice.isHigherThan(currentPrice);
    }
}
