package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.model.Wallet;

public interface WalletCommandPort {

    Wallet save(Wallet wallet);
}
