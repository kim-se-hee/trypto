package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshot;

public record ExchangeCoinListResult(
    Long exchangeCoinId,
    Long coinId,
    String coinSymbol,
    String coinName,
    TickerSnapshot tickerSnapshot
) {
}
