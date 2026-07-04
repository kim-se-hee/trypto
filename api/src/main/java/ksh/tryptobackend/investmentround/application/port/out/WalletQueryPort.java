package ksh.tryptobackend.investmentround.application.port.out;

/** investmentround 가 wallet 컨텍스트의 데이터를 조회하기 위한 포트. */
public interface WalletQueryPort {

    Long getRoundId(Long walletId);

    Long getWalletId(Long roundId, Long exchangeId);
}
