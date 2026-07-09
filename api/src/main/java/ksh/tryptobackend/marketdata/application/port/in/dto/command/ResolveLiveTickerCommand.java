package ksh.tryptobackend.marketdata.application.port.in.dto.command;

import java.math.BigDecimal;
import java.util.List;

public record ResolveLiveTickerCommand(String exchange, List<ExternalTicker> tickers) {

    public record ExternalTicker(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            BigDecimal quoteTurnover,
            Long timestamp) {}
}
