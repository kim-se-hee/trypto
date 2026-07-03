package ksh.tryptobackend.trading.domain.service;

import java.util.List;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;

public interface WalletBalanceService {

    void apply(Long walletId, BalanceChange change);

    void applyAll(Long walletId, List<BalanceChange> changes);
}
