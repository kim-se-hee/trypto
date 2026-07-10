package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletCommandAdapter implements WalletCommandPort {

    private final WalletJpaRepository walletRepository;

    @Override
    public Wallet save(Wallet wallet) {
        return walletRepository.save(WalletJpaEntity.fromDomain(wallet)).toDomain();
    }
}
