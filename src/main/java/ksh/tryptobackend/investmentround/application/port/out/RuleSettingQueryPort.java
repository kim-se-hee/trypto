package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.domain.model.RuleSetting;

import java.util.List;

public interface RuleSettingQueryPort {

    List<RuleSetting> findByRoundId(Long roundId);
}
