package ksh.tryptocollector.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * ticker.exchange 채널의 발행 단위. 같은 거래소의 1 윈도우(50ms) batch.
 * 페이로드 약속은 docs/contracts/ticker-exchange.md.
 */
public record TickerBatchEvent(String exchange, List<Item> tickers) {
    public record Item(
            String symbol, BigDecimal currentPrice, BigDecimal changeRate, BigDecimal quoteTurnover, long timestamp) {
        public static Item from(NormalizedTicker t) {
            return new Item(t.base() + "/" + t.quote(), t.lastPrice(), t.changeRate(), t.quoteTurnover(), t.tsMs());
        }
    }
}
