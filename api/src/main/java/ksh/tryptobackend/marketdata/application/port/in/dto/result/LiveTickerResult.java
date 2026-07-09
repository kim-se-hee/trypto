package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ExternalTickerCommand;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;

public record LiveTickerResult(
        Long exchangeId,
        Long coinId,
        String symbol,
        BigDecimal price,
        BigDecimal changeRate,
        BigDecimal quoteTurnover,
        Long timestamp) {

    public static LiveTickerResult of(ExchangeCoinMapping mapping, ExternalTickerCommand ticker) {
        return new LiveTickerResult(
                mapping.exchangeId(),
                mapping.coinId(),
                mapping.coinSymbol(),
                ticker.currentPrice(),
                ticker.changeRate(),
                ticker.quoteTurnover(),
                ticker.timestamp());
    }
}
