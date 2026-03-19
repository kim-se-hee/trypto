package ksh.tryptobackend.marketdata.domain.model;

public enum ExchangeMarketType {
    DOMESTIC,
    OVERSEAS;

    public boolean isDomestic() {
        return this == DOMESTIC;
    }
}
