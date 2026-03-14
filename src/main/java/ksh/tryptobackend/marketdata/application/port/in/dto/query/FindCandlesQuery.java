package ksh.tryptobackend.marketdata.application.port.in.dto.query;

public record FindCandlesQuery(
    String exchange,
    String coin,
    String interval,
    Integer limit,
    String cursor
) {
}
