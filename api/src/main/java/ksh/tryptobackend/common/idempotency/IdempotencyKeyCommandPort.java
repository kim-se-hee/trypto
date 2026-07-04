package ksh.tryptobackend.common.idempotency;

import java.time.LocalDateTime;

/** 여러 커맨드가 공유하는 전역 멱등성 키의 선점·연결 포트. */
public interface IdempotencyKeyCommandPort {

    void preempt(String idempotencyKey, IdempotencyResourceType resourceType, LocalDateTime now);

    void linkResource(String idempotencyKey, Long resourceId);
}
