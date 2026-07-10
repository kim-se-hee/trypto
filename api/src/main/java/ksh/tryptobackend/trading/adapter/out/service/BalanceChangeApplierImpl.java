package ksh.tryptobackend.trading.adapter.out.service;

import java.util.List;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceChangeApplierImpl implements BalanceChangeApplier {

    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @Override
    public void applyAll(Long walletId, List<BalanceChange> changes) {
        changes.forEach(change -> apply(walletId, change));
    }

    @Override
    public void apply(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.AddAvailable a ->
                    manageWalletBalanceUseCase.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l ->
                    manageWalletBalanceUseCase.lockBalance(walletId, l.coinId(), l.amount());
            case BalanceChange.Unlock u ->
                    manageWalletBalanceUseCase.unlockBalance(walletId, u.coinId(), u.amount());
            case BalanceChange.ConsumeLocked s ->
                    manageWalletBalanceUseCase.consumeLocked(walletId, s.coinId(), s.amount());
        }
    }
}
