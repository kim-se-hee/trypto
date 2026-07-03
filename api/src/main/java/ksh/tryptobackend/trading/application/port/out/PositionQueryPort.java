package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.domain.model.Position;

public interface PositionQueryPort {

    List<Position> findAllByWalletId(Long walletId);

    Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
