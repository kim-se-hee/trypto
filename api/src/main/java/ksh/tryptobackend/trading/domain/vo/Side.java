package ksh.tryptobackend.trading.domain.vo;

public enum Side {
    BUY,
    SELL;

    public boolean canFillAt(Price limitPrice, Price executionPrice) {
        return this == BUY ? !executionPrice.isHigherThan(limitPrice) : !limitPrice.isHigherThan(executionPrice);
    }
}
