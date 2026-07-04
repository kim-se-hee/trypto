package ksh.tryptobackend.investmentround.adapter.out;

import java.util.List;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRuleJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRuleJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.RuleQueryPort;
import ksh.tryptobackend.investmentround.domain.model.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleQueryAdapter implements RuleQueryPort {

    private final InvestmentRuleJpaRepository repository;

    @Override
    public List<Rule> findByRoundId(Long roundId) {
        return repository.findByRoundId(roundId).stream()
                .map(InvestmentRuleJpaEntity::toRoundDomain)
                .toList();
    }
}
