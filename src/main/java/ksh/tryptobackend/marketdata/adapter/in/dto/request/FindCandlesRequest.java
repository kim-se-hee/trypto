package ksh.tryptobackend.marketdata.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;

public record FindCandlesRequest(
    @NotBlank String exchange,
    @NotBlank String coin,
    @NotBlank String interval,
    @Min(1) @Max(200) Integer limit,
    String cursor
) {

    public FindCandlesQuery toQuery() {
        return new FindCandlesQuery(exchange, coin, interval, limit, cursor);
    }
}
