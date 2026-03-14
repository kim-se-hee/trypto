package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WalletBalanceQueryAdapter implements WalletBalanceQueryPort {

    private final WalletBalanceJpaRepository walletBalanceRepository;

    @Override
    public List<WalletBalance> findByWalletId(Long walletId) {
        return walletBalanceRepository.findByWalletId(walletId).stream()
            .map(WalletBalanceJpaEntity::toDomain)
            .toList();
    }
}
