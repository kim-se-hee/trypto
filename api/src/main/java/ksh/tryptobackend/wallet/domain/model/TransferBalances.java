package ksh.tryptobackend.wallet.domain.model;

import java.util.List;

public record TransferBalances(WalletBalance source, WalletBalance destination) {

    public List<WalletBalance> toList() {
        return List.of(source, destination);
    }
}
