package ksh.tryptobackend.common.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfig {

    private static final int BATCH_THREAD_POOL_SIZE = 2;

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(BATCH_THREAD_POOL_SIZE);
        executor.setMaxPoolSize(BATCH_THREAD_POOL_SIZE);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
}
