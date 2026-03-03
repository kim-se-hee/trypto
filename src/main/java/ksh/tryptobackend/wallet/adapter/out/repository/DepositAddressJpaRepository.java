package ksh.tryptobackend.wallet.adapter.out.repository;

import ksh.tryptobackend.wallet.adapter.out.entity.DepositAddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DepositAddressJpaRepository extends JpaRepository<DepositAddressJpaEntity, Long> {

    Optional<DepositAddressJpaEntity> findByWalletIdAndChain(Long walletId, String chain);

    @Query("SELECT da FROM DepositAddressJpaEntity da " +
        "JOIN WalletJpaEntity w ON da.walletId = w.id " +
        "WHERE w.roundId = :roundId AND da.chain = :chain AND da.address = :address")
    Optional<DepositAddressJpaEntity> findByRoundIdAndChainAndAddress(
        Long roundId, String chain, String address);
}
