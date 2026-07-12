package ksh.tryptobackend.common.idempotency;

import java.time.LocalDateTime;

public interface IdempotencyKeyCommandPort {

    void preempt(String idempotencyKey, IdempotencyResourceType resourceType, LocalDateTime now);

    void linkResource(String idempotencyKey, Long resourceId);
}
