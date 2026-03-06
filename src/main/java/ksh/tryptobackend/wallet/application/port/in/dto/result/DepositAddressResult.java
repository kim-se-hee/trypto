package ksh.tryptobackend.wallet.application.port.in.dto.result;

public record DepositAddressResult(Long walletId, String chain, String address, String tag) {
}
