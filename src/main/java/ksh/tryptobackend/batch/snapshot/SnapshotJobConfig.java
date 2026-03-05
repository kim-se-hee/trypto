package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.batch.common.LoggingSkipListener;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SnapshotJobConfig {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job snapshotJob(Step snapshotStep) {
        return new JobBuilder("snapshot-job", jobRepository)
            .start(snapshotStep)
            .build();
    }

    @Bean
    public Step snapshotStep(SnapshotItemReader reader,
                             SnapshotItemProcessor processor,
                             SnapshotItemWriter writer,
                             RetryPolicy batchRetryPolicy,
                             SkipPolicy batchSkipPolicy,
                             LoggingSkipListener<Object, Object> batchSkipListener) {
        return new StepBuilder("snapshot-step", jobRepository)
            .<SnapshotInput, SnapshotResult>chunk(CHUNK_SIZE)
            .transactionManager(transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .retryPolicy(batchRetryPolicy)
            .skipPolicy(batchSkipPolicy)
            .listener(batchSkipListener)
            .build();
    }
}
