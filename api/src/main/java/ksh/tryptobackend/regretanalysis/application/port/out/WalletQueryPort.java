package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisWallet;

public interface WalletQueryPort {

    List<AnalysisWallet> findWallets(Long roundId);
}
