package ksh.tryptobackend.transfer.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.transfer.domain.model.Transfer;

public interface TransferCommandPort {

    Transfer save(Transfer transfer);

    Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey);
}
