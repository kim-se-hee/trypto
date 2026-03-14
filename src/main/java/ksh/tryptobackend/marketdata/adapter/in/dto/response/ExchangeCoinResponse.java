package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;

public record ExchangeCoinResponse(
    Long exchangeCoinId,
    Long coinId,
    String coinSymbol,
    String coinName
) {

    public static ExchangeCoinResponse from(ExchangeCoinListResult result) {
        return new ExchangeCoinResponse(
            result.exchangeCoinId(),
            result.coinId(),
            result.coinSymbol(),
            result.coinName()
        );
    }
}
