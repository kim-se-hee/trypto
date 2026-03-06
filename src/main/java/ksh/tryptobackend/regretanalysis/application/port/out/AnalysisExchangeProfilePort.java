package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchangeProfile;

public interface AnalysisExchangeProfilePort {

    AnalysisExchangeProfile getExchangeProfile(Long exchangeId);

    boolean existsWalletForExchange(Long roundId, Long exchangeId);
}
