package ksh.tryptobackend.wallet.application.service;

import java.util.List;
import ksh.tryptobackend.wallet.application.port.in.ApplyBalanceChangesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.BalanceChangeItem;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import ksh.tryptobackend.wallet.domain.model.WalletBalances;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplyBalanceChangesService implements ApplyBalanceChangesUseCase {

    private final WalletBalanceQueryPort walletBalanceQueryPort;
    private final WalletBalanceCommandPort walletBalanceCommandPort;

    @Override
    @Transactional
    public void applyBalanceChanges(Long walletId, List<BalanceChangeItem> changes) {
        if (changes.isEmpty()) {
            return;
        }

        List<Long> coinIds = changes.stream().map(BalanceChangeItem::coinId).distinct().toList();
        List<WalletBalance> lockedBalances =
                walletBalanceQueryPort.getAllByWalletIdAndCoinIdsWithLock(walletId, coinIds);
        WalletBalances balances = new WalletBalances(lockedBalances);

        changes.forEach(
                change ->
                        change.type()
                                .apply(balances.getByCoinId(change.coinId()), change.amount()));
        walletBalanceCommandPort.saveAll(lockedBalances);
    }
}
