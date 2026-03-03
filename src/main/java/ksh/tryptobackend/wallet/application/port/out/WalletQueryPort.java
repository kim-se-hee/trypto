package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;

import java.util.Optional;

public interface WalletQueryPort {

    Optional<WalletInfo> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    Optional<WalletInfo> findById(Long walletId);
}
