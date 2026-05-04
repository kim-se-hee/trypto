package ksh.tryptobackend.investmentround.application.port.out;

import java.util.List;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;

public interface RuleSettingQueryPort {

    List<RuleSetting> findByRoundId(Long roundId);
}
