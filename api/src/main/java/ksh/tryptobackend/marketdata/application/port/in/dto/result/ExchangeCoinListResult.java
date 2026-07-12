package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshot;

public record ExchangeCoinListResult(
        Long exchangeCoinId, Long coinId, String coinSymbol, String coinName, TickerSnapshot tickerSnapshot) {

    public static ExchangeCoinListResult of(
            ExchangeCoin exchangeCoin, String coinSymbol, TickerSnapshot tickerSnapshot) {
        return new ExchangeCoinListResult(
                exchangeCoin.exchangeCoinId(),
                exchangeCoin.coinId(),
                coinSymbol,
                exchangeCoin.displayName(),
                tickerSnapshot);
    }
}
