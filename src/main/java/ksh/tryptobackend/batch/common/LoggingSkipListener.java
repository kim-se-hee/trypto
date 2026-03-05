package ksh.tryptobackend.batch.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;

@Slf4j
public class LoggingSkipListener<T, S> implements SkipListener<T, S> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP-READ] 읽기 중 스킵 발생: {}", t.getMessage(), t);
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("[SKIP-PROCESS] 처리 중 스킵 발생: item={}, error={}", item, t.getMessage(), t);
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("[SKIP-WRITE] 쓰기 중 스킵 발생: item={}, error={}", item, t.getMessage(), t);
    }
}
