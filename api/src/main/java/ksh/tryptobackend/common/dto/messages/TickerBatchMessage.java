package ksh.tryptobackend.common.dto.messages;

import java.math.BigDecimal;
import java.util.List;

/**
 * collector 의 ticker.exchange 채널 페이로드.
 * 1 메시지 = 1 거래소의 50ms 윈도우 batch.
 * 스키마는 docs/contracts/ticker-exchange.md 참조.
 */
public record TickerBatchMessage(
        String exchange,
        List<Item> tickers
) {
    public record Item(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            BigDecimal quoteTurnover,
            Long timestamp
    ) {}
}
