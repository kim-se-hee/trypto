package ksh.tryptobackend.common.idempotency;

import java.util.Optional;

public interface IdempotencyKeyQueryPort {

    Optional<Long> findResourceId(String idempotencyKey);
}
