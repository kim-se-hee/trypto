package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.Holding;

import java.util.Optional;

public interface HoldingCommandPort {

    Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Holding save(Holding holding);
}
