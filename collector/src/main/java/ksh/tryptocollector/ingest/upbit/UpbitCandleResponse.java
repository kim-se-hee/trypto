package ksh.tryptocollector.ingest.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record UpbitCandleResponse(
        @JsonProperty("candle_date_time_utc") String candleDateTimeUtc,
        @JsonProperty("opening_price") BigDecimal openingPrice,
        @JsonProperty("high_price") BigDecimal highPrice,
        @JsonProperty("low_price") BigDecimal lowPrice,
        @JsonProperty("trade_price") BigDecimal tradePrice,
        long timestamp) {

    public long startMs() {
        return LocalDateTime.parse(candleDateTimeUtc).toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
