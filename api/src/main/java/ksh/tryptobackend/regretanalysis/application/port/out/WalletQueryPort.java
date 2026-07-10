package ksh.tryptobackend.regretanalysis.application.port.out;

public interface WalletQueryPort {

    boolean existsWallet(Long roundId, Long exchangeId);
}
