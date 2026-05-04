package ksh.tryptobackend.trading.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.trading.domain.model.Holding;

public interface HoldingCommandPort {

    Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Holding save(Holding holding);
}
