package ksh.tryptobackend.wallet.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;

public interface DepositAddressQueryPort {

    Optional<DepositAddress> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Optional<DepositAddress> findByRoundIdAndAddress(Long roundId, String address);
}
