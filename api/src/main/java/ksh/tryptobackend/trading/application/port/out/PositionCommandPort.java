package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.TransferPositions;

public interface PositionCommandPort {

    Position getOrCreate(Long walletId, Long coinId);

    TransferPositions getTransferPositionsWithLock(Long coinId, Long fromWalletId, Long toWalletId);

    Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId);

    Position save(Position position);

    void saveAll(List<Position> positions);
}
