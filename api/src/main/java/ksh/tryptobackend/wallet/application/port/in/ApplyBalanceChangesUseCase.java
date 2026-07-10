package ksh.tryptobackend.wallet.application.port.in;

import java.util.List;
import ksh.tryptobackend.wallet.application.port.in.dto.command.BalanceChangeItem;

public interface ApplyBalanceChangesUseCase {

    void applyBalanceChanges(Long walletId, List<BalanceChangeItem> changes);
}
