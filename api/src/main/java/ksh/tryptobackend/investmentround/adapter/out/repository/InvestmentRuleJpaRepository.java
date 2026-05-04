package ksh.tryptobackend.investmentround.adapter.out.repository;

import java.util.List;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRuleJpaRepository extends JpaRepository<InvestmentRuleJpaEntity, Long> {

    List<InvestmentRuleJpaEntity> findByRoundId(Long roundId);
}
