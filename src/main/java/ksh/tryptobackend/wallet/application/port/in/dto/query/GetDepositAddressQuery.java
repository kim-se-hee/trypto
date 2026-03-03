package ksh.tryptobackend.wallet.application.port.in.dto.query;

public record GetDepositAddressQuery(Long walletId, Long coinId, String chain) {
}
