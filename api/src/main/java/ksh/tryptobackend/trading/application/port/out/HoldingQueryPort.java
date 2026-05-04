package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.domain.model.Holding;

public interface HoldingQueryPort {

    List<Holding> findAllByWalletId(Long walletId);

    Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
