package ksh.tryptobackend.marketdata.domain.vo;

public record ExchangeCoinMapping(
    Long exchangeCoinId,
    Long exchangeId,
    Long coinId,
    String coinSymbol
) {
}
