package ksh.tryptobackend.common.idempotency;

import java.util.Optional;

/** 멱등성 키에 연결된 리소스 식별자 조회 포트. 중복 재요청 응답 재구성에 쓴다. */
public interface IdempotencyKeyQueryPort {

    Optional<Long> findResourceId(String idempotencyKey);
}
