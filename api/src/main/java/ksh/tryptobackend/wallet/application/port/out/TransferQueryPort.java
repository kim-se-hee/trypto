package ksh.tryptobackend.wallet.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferType;

public interface TransferQueryPort {

    Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey);

    List<Transfer> findByCursor(Long walletId, TransferType type, Long cursorTransferId, int size);
}
