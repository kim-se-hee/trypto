package ksh.tryptobackend.marketdata.application.port.in.dto.result;

public record ExchangeCoinListResult(
    Long exchangeCoinId,
    Long coinId,
    String coinSymbol,
    String coinName
) {
}
