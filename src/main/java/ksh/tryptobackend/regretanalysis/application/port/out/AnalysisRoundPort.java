package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;

public interface AnalysisRoundPort {

    AnalysisRound getRound(Long roundId);
}
