package ksh.tryptobackend.wallet.adapter.out.persistence.repository;

import ksh.tryptobackend.wallet.adapter.out.persistence.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {}
