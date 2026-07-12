package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import ksh.tryptobackend.marketdata.domain.model.Candle;

public record CandleResponse(Instant time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {

    public static CandleResponse from(Candle candle) {
        return new CandleResponse(candle.time(), candle.open(), candle.high(), candle.low(), candle.close());
    }
}
