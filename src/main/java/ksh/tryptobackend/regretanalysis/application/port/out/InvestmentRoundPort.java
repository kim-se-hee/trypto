package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundInfoResult;

public interface InvestmentRoundPort {

    RoundInfoResult getRound(Long roundId);
}
