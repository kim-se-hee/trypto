package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisWallet;

public interface WalletQueryPort {

    boolean existsWallet(Long roundId, Long exchangeId);

    List<AnalysisWallet> findWallets(Long roundId);
}
