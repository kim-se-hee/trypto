package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisActiveRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;

public interface InvestmentRoundQueryPort {

    AnalysisRound getRound(Long roundId);

    AnalysisRules findRules(Long roundId);

    List<AnalysisActiveRound> findActiveRounds();
}
