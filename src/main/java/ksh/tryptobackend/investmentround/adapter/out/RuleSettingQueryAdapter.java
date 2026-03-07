package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRuleJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRuleJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.RuleSettingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleSettingQueryAdapter implements RuleSettingQueryPort {

    private final InvestmentRuleJpaRepository repository;

    @Override
    public List<RuleSetting> findByRoundId(Long roundId) {
        return repository.findByRoundId(roundId).stream()
            .map(InvestmentRuleJpaEntity::toRoundDomain)
            .toList();
    }
}
