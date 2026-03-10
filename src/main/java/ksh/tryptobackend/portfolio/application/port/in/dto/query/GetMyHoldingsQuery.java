package ksh.tryptobackend.portfolio.application.port.in.dto.query;

public record GetMyHoldingsQuery(
    Long userId,
    Long walletId
) {
}
