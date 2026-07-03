package ksh.tryptobackend.common.dto.messages;

import java.math.BigDecimal;
import java.util.List;

public record TickerBatchMessage(String exchange, List<Item> tickers) {
    public record Item(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            BigDecimal quoteTurnover,
            Long timestamp) {}
}
