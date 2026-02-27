package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRulePersistencePort;
import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvestmentRuleJpaPersistenceAdapter implements InvestmentRulePersistencePort, InvestmentRulePort {

    private final InvestmentRuleJpaRepository repository;

    @Override
    public List<ksh.tryptobackend.investmentround.domain.model.InvestmentRule> saveAll(
        List<ksh.tryptobackend.investmentround.domain.model.InvestmentRule> rules
    ) {
        List<InvestmentRuleJpaEntity> entities = rules.stream()
            .map(InvestmentRuleJpaEntity::fromDomain)
            .toList();

        return repository.saveAll(entities).stream()
            .map(InvestmentRuleJpaEntity::toRoundDomain)
            .toList();
    }

    @Override
    public List<ksh.tryptobackend.trading.domain.model.InvestmentRule> findByRoundId(Long roundId) {
        return repository.findByRoundId(roundId).stream()
            .map(entity -> ksh.tryptobackend.trading.domain.model.InvestmentRule.of(
                entity.getId(), entity.getRuleType(), entity.getThresholdValue()
            ))
            .toList();
    }
}
