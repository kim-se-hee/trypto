package ksh.tryptobackend.transfer.application.port.out;

public interface TransferWalletPort {

    Long getOwnerUserId(Long walletId);
}
