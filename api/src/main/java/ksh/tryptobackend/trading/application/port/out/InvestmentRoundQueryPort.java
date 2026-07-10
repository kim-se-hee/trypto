package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import ksh.tryptobackend.trading.domain.vo.InvestmentRule;

public interface InvestmentRoundQueryPort {

    List<InvestmentRule> findRulesByRoundId(Long roundId);
}
