package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.wallet.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.TransferCommandPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
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
}
