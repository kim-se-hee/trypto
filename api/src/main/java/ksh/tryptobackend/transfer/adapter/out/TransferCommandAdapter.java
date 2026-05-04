package ksh.tryptobackend.transfer.adapter.out;

import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.transfer.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.transfer.application.port.out.TransferCommandPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferCommandAdapter implements TransferCommandPort {

    private final TransferJpaRepository repository;

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity entity = TransferJpaEntity.fromDomain(transfer);
        TransferJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(TransferJpaEntity::toDomain);
    }
}
