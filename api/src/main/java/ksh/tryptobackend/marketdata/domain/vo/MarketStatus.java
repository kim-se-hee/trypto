package ksh.tryptobackend.marketdata.domain.vo;

public enum MarketStatus {
    TRADING,
    SUSPENDED;

    public boolean isTrading() {
        return this == TRADING;
    }

    public boolean isSuspended() {
        return this == SUSPENDED;
    }
}
