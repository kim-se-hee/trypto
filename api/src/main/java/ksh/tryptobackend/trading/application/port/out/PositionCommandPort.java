package ksh.tryptobackend.trading.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.trading.domain.model.Position;

public interface PositionCommandPort {

    Position getOrCreate(Long walletId, Long coinId);

    Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Position save(Position position);
}
