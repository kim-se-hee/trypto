package ksh.tryptobackend.trading.domain.service;

public interface WalletOwnershipVerifier {

    void verify(Long walletId, Long requesterId);
}
