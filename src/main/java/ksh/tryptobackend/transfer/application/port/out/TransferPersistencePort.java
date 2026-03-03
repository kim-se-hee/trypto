package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferPersistencePort {

    Transfer save(Transfer transfer);

    Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey);

    List<Transfer> findByCursor(Long walletId, TransferType type, Long cursorTransferId, int size);
}
