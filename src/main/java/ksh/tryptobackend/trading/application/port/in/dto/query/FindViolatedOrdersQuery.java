package ksh.tryptobackend.trading.application.port.in.dto.query;

public record FindViolatedOrdersQuery(Long roundId, Long exchangeId, Long walletId) {
}
