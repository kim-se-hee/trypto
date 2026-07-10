package ksh.tryptobackend.wallet.application.port.out;

import java.util.List;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;

public interface WalletBalanceQueryPort {

    List<WalletBalance> findByWalletId(Long walletId);

    List<WalletBalance> getAllByWalletIdAndCoinIdsWithLock(Long walletId, List<Long> coinIds);

    List<WalletBalance> getAllByCoinIdAndWalletIdsWithLock(Long coinId, List<Long> walletIds);
}
