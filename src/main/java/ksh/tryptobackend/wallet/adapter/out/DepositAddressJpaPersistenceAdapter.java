package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.DepositAddressJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.DepositAddressJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressPersistencePort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.DepositAddressInfo;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepositAddressJpaPersistenceAdapter implements DepositAddressPersistencePort, DepositAddressQueryPort {

    private final DepositAddressJpaRepository repository;

    @Override
    public Optional<DepositAddress> findByWalletIdAndChain(Long walletId, String chain) {
        return repository.findByWalletIdAndChain(walletId, chain)
            .map(DepositAddressJpaEntity::toDomain);
    }

    @Override
    public DepositAddress save(DepositAddress depositAddress) {
        DepositAddressJpaEntity entity = DepositAddressJpaEntity.fromDomain(depositAddress);
        DepositAddressJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<DepositAddressInfo> findByRoundIdAndChainAndAddress(
        Long roundId, String chain, String address) {
        return repository.findByRoundIdAndChainAndAddress(roundId, chain, address)
            .map(entity -> new DepositAddressInfo(
                entity.getId(), entity.getWalletId(), entity.getChain(),
                entity.getAddress(), entity.getTag()));
    }
}
