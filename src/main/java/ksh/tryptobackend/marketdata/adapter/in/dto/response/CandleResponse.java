package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.domain.model.Candle;

import java.time.Instant;

public record CandleResponse(
    Instant time,
    double open,
    double high,
    double low,
    double close
) {

    public static CandleResponse from(Candle candle) {
        return new CandleResponse(
            candle.time(), candle.open(), candle.high(), candle.low(), candle.close());
    }
}
