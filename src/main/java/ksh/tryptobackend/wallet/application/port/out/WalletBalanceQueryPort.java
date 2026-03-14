package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.model.WalletBalance;

import java.util.List;

public interface WalletBalanceQueryPort {

    List<WalletBalance> findByWalletId(Long walletId);
}
