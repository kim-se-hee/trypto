package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferWalletInfo;

public interface TransferWalletPort {

    TransferWalletInfo getWallet(Long walletId);
}
