package ksh.tryptobackend.investmentround.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.domain.vo.IdempotencyResourceType;

public interface IdempotencyKeyCommandPort {

    void preempt(UUID idempotencyKey, IdempotencyResourceType resourceType, LocalDateTime now);

    void linkResource(UUID idempotencyKey, Long resourceId);
}
