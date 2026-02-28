package ksh.tryptobackend.investmentround.adapter.out.repository;

import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestmentRoundJpaRepository extends JpaRepository<InvestmentRoundJpaEntity, Long> {

    boolean existsByUserIdAndStatus(Long userId, RoundStatus status);

    long countByUserId(Long userId);

    Optional<InvestmentRoundJpaEntity> findByUserIdAndStatus(Long userId, RoundStatus status);
}
