package ksh.tryptobackend.investmentround.application.port.out;

import java.util.List;
import ksh.tryptobackend.investmentround.domain.vo.RoundWallet;

public interface WalletQueryPort {

    Long getRoundId(Long walletId);

    Long getWalletId(Long roundId, Long exchangeId);

    List<RoundWallet> findWalletsByRoundId(Long roundId);
}
