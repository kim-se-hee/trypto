package ksh.tryptobackend.marketdata.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;

import java.time.Instant;

public record FindCandlesRequest(
    @NotBlank String exchange,
    @NotBlank String coin,
    @NotBlank String interval,
    @Min(1) @Max(200) Integer limit,
    String cursor
) {

    public FindCandlesRequest {
        if (limit == null) {
            limit = 60;
        }
    }

    public FindCandlesQuery toQuery() {
        CandleInterval candleInterval = CandleInterval.of(interval);
        Instant cursorInstant = (cursor != null && !cursor.isBlank()) ? Instant.parse(cursor) : null;
        return new FindCandlesQuery(exchange, coin, candleInterval, limit, cursorInstant);
    }
}
