package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.transfer.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.transfer.application.port.out.TransferPersistencePort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferJpaPersistenceAdapter implements TransferPersistencePort {

    private final TransferJpaRepository repository;

    @Override
    public Transfer save(Transfer transfer) {
        TransferJpaEntity entity = TransferJpaEntity.fromDomain(transfer);
        TransferJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(UUID idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
            .map(TransferJpaEntity::toDomain);
    }

    @Override
    public List<Transfer> findByCursor(Long walletId, TransferType type, Long cursorTransferId, int size) {
        throw new UnsupportedOperationException("QueryDSL 구현 예정");
    }
}
