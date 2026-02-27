package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRoundJpaRepository extends JpaRepository<InvestmentRoundJpaEntity, Long> {

    boolean existsByUserIdAndStatus(Long userId, RoundStatus status);

    long countByUserId(Long userId);
}
