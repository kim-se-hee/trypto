package ksh.tryptobackend.batch.common;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;

import java.time.Duration;

@Configuration
public class BatchFaultTolerancePolicy {

    private static final int RETRY_LIMIT = 5;
    private static final long INITIAL_INTERVAL = 200L;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_INTERVAL = 5000L;

    @Bean
    public RetryPolicy batchRetryPolicy() {
        return RetryPolicy.builder()
            .maxRetries(RETRY_LIMIT)
            .delay(Duration.ofMillis(INITIAL_INTERVAL))
            .multiplier(MULTIPLIER)
            .maxDelay(Duration.ofMillis(MAX_INTERVAL))
            .includes(TransientDataAccessException.class)
            .build();
    }

    @Bean
    public SkipPolicy batchSkipPolicy() {
        return new BatchSkipPolicy();
    }

    @Bean
    public LoggingSkipListener<Object, Object> batchSkipListener() {
        return new LoggingSkipListener<>();
    }
}
