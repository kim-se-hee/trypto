package ksh.tryptobackend.investmentround.adapter.out;

import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.domain.vo.IdempotencyResourceType;
import ksh.tryptobackend.common.exception.DuplicateRequestException;
import ksh.tryptobackend.investmentround.adapter.out.entity.IdempotencyKeyJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.IdempotencyKeyJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.IdempotencyKeyCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** 멱등성 키의 유니크 위반을 중복 요청 신호로 번역하는 어댑터. 도메인·애플리케이션은 인프라 예외를 보지 않는다. */
@Component
@RequiredArgsConstructor
public class IdempotencyKeyCommandAdapter implements IdempotencyKeyCommandPort {

    private final IdempotencyKeyJpaRepository repository;

    @Override
    public void preempt(
            UUID idempotencyKey, IdempotencyResourceType resourceType, LocalDateTime now) {
        try {
            repository.saveAndFlush(
                    IdempotencyKeyJpaEntity.preempt(idempotencyKey, resourceType, now));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRequestException();
        }
    }

    @Override
    public void linkResource(UUID idempotencyKey, Long resourceId) {
        IdempotencyKeyJpaEntity entity =
                repository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "preempted idempotency key must exist: "
                                                        + idempotencyKey));
        entity.assignResource(resourceId);
    }
}
