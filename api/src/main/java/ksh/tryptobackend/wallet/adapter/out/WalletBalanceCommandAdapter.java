package ksh.tryptobackend.wallet.adapter.out;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceCommandPort;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletBalanceCommandAdapter implements WalletBalanceCommandPort {

    private final WalletBalanceJpaRepository walletBalanceRepository;

    @Override
    public void saveAll(List<WalletBalance> balances) {
        balances.forEach(this::save);
    }

    private void save(WalletBalance balance) {
        if (balance.getId() == null) {
            walletBalanceRepository.save(
                    new WalletBalanceJpaEntity(
                            balance.getWalletId(),
                            balance.getCoinId(),
                            balance.getAvailable(),
                            balance.getLocked()));
            return;
        }
        WalletBalanceJpaEntity entity =
                walletBalanceRepository
                        .findById(balance.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_BALANCE_NOT_FOUND));
        entity.updateFrom(balance);
    }
}
