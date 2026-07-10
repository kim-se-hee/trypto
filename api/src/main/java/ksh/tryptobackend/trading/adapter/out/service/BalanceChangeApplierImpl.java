package ksh.tryptobackend.trading.adapter.out.service;

import java.util.List;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.wallet.application.port.in.ApplyBalanceChangesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.BalanceChangeItem;
import ksh.tryptobackend.wallet.application.port.in.dto.command.BalanceChangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceChangeApplierImpl implements BalanceChangeApplier {

    private final ApplyBalanceChangesUseCase applyBalanceChangesUseCase;

    @Override
    public void applyAll(Long walletId, List<BalanceChange> changes) {
        applyBalanceChangesUseCase.applyBalanceChanges(
                walletId, changes.stream().map(this::translate).toList());
    }

    @Override
    public void apply(Long walletId, BalanceChange change) {
        applyAll(walletId, List.of(change));
    }

    private BalanceChangeItem translate(BalanceChange change) {
        return switch (change) {
            case BalanceChange.AddAvailable a ->
                    new BalanceChangeItem(BalanceChangeType.ADD_AVAILABLE, a.coinId(), a.amount());
            case BalanceChange.Lock l ->
                    new BalanceChangeItem(BalanceChangeType.LOCK, l.coinId(), l.amount());
            case BalanceChange.Unlock u ->
                    new BalanceChangeItem(BalanceChangeType.UNLOCK, u.coinId(), u.amount());
            case BalanceChange.ConsumeLocked s ->
                    new BalanceChangeItem(BalanceChangeType.CONSUME_LOCKED, s.coinId(), s.amount());
        };
    }
}
