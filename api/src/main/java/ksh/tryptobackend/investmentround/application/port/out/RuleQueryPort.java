package ksh.tryptobackend.investmentround.application.port.out;

import java.util.List;
import ksh.tryptobackend.investmentround.domain.model.Rule;

public interface RuleQueryPort {

    List<Rule> findByRoundId(Long roundId);
}
