package ksh.tryptobackend.investmentround.adapter.out.persistence.repository;

import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.EmergencyFundingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyFundingJpaRepository
        extends JpaRepository<EmergencyFundingJpaEntity, Long> {}
