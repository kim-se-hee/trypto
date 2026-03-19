package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshot;

import java.math.BigDecimal;

public record ExchangeCoinResponse(
    Long exchangeCoinId,
    Long coinId,
    String coinSymbol,
    String coinName,
    BigDecimal price,
    BigDecimal changeRate,
    BigDecimal volume
) {

    public static ExchangeCoinResponse from(ExchangeCoinListResult result) {
        TickerSnapshot ticker = result.tickerSnapshot();
        return new ExchangeCoinResponse(
            result.exchangeCoinId(),
            result.coinId(),
            result.coinSymbol(),
            result.coinName(),
            ticker.price(),
            ticker.changeRate(),
            ticker.volume()
        );
    }
}
