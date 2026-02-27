package ksh.tryptobackend.wallet.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {
}
