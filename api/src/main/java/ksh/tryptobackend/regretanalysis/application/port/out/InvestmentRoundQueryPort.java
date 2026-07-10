package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;

public interface InvestmentRoundQueryPort {

    AnalysisRound getRound(Long roundId);

    AnalysisRules findRules(Long roundId);
}
