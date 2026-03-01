package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleInfo;

import java.util.List;

public interface InvestmentRulePort {

    List<RuleInfo> findByRoundId(Long roundId);
}
