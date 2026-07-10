package ksh.tryptobackend.investmentround.adapter.out.persistence.repository;

import java.util.List;
import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.InvestmentRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRuleJpaRepository extends JpaRepository<InvestmentRuleJpaEntity, Long> {

    List<InvestmentRuleJpaEntity> findByRoundId(Long roundId);
}
