package ksh.tryptobackend.trading.application.port.in.dto.command;

public record CancelOrderCommand(
    Long orderId,
    Long walletId
) {
}
