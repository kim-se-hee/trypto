package ksh.tryptobackend.trading.application.port.out;

public interface WalletQueryPort {

    Long getOwnerId(Long walletId);
}
