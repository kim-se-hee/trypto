package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;

public record TickerResponse(
        Long coinId, String symbol, BigDecimal price, BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp) {

    public static TickerResponse from(LiveTickerResult result) {
        return new TickerResponse(
                result.coinId(),
                result.symbol(),
                result.price(),
                result.changeRate(),
                result.quoteTurnover(),
                result.timestamp());
    }
}
