package ksh.tryptobackend.wallet.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;

public interface WalletBalanceQueryPort {

    List<WalletBalance> findByWalletId(Long walletId);

    Optional<WalletBalance> findByWalletIdAndCoinId(Long walletId, Long coinId);

    List<WalletBalance> getAllByWalletIdAndCoinIdsWithLock(Long walletId, List<Long> coinIds);

    List<WalletBalance> getAllByCoinIdAndWalletIdsWithLock(Long coinId, List<Long> walletIds);
}
