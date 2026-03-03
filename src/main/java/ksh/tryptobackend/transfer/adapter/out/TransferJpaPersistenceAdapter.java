package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.transfer.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.transfer.application.port.out.TransferPersistencePort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
    public Page<Transfer> findByFromWalletId(Long walletId, Pageable pageable) {
        return repository.findByFromWalletId(walletId, pageable)
            .map(TransferJpaEntity::toDomain);
    }

    @Override
    public Page<Transfer> findByToWalletId(Long walletId, Pageable pageable) {
        return repository.findByToWalletId(walletId, pageable)
            .map(TransferJpaEntity::toDomain);
    }

    @Override
    public Page<Transfer> findByWalletId(Long walletId, Pageable pageable) {
        return repository.findByFromWalletIdOrToWalletId(walletId, pageable)
            .map(TransferJpaEntity::toDomain);
    }
}
