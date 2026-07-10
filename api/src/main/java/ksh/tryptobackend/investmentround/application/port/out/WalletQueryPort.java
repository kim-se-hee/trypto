package ksh.tryptobackend.investmentround.application.port.out;

import java.util.List;
import ksh.tryptobackend.investmentround.domain.vo.RoundWallet;

/** investmentround 가 wallet 컨텍스트의 데이터를 조회하기 위한 포트. */
public interface WalletQueryPort {

    Long getRoundId(Long walletId);

    Long getWalletId(Long roundId, Long exchangeId);

    List<RoundWallet> findWalletsByRoundId(Long roundId);
}
