package ksh.tryptobackend.wallet.adapter.out.repository;

import ksh.tryptobackend.wallet.adapter.out.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {}
