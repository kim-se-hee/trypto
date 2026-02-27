package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRulePersistencePort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRuleQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRuleInfo;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvestmentRuleJpaPersistenceAdapter implements InvestmentRulePersistencePort, InvestmentRuleQueryPort {

    private final InvestmentRuleJpaRepository repository;

    @Override
    public List<InvestmentRule> saveAll(List<InvestmentRule> rules) {
        List<InvestmentRuleJpaEntity> entities = rules.stream()
            .map(InvestmentRuleJpaEntity::fromDomain)
            .toList();

        return repository.saveAll(entities).stream()
            .map(InvestmentRuleJpaEntity::toRoundDomain)
            .toList();
    }

    @Override
    public List<InvestmentRuleInfo> findByRoundId(Long roundId) {
        return repository.findByRoundId(roundId).stream()
            .map(entity -> new InvestmentRuleInfo(
                entity.getId(), entity.getRuleType(), entity.getThresholdValue()
            ))
            .toList();
    }
}
