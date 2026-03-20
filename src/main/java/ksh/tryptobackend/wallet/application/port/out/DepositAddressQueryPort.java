package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.model.DepositAddress;

import java.util.Optional;

public interface DepositAddressQueryPort {

    Optional<DepositAddress> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Optional<DepositAddress> findByRoundIdAndAddress(Long roundId, String address);
}
