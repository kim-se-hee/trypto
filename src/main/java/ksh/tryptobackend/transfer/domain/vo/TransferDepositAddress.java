package ksh.tryptobackend.transfer.domain.vo;

public record TransferDepositAddress(Long walletId, String chain, String address, String tag) {
}
