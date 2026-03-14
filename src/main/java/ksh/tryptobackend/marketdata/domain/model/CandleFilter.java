package ksh.tryptobackend.marketdata.domain.model;

import java.time.Instant;

public record CandleFilter(
    String exchange,
    String coin,
    CandleInterval interval,
    int limit,
    Instant cursor
) {
}
