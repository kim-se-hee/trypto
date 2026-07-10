package ksh.tryptobackend.common.idempotency;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IdempotencyKeyQueryAdapter implements IdempotencyKeyQueryPort {

    private final IdempotencyKeyJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findResourceId(String idempotencyKey) {
        return repository
                .findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyKeyJpaEntity::getResourceId);
    }
}
