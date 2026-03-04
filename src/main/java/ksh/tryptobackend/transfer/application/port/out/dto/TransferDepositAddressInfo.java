package ksh.tryptobackend.transfer.application.port.out.dto;

public record TransferDepositAddressInfo(Long walletId, String chain, String address, String tag) {
}
