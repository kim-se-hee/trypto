package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.batch.common.LoggingSkipListener;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
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
public class RegretReportJobConfig {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job regretReportJob(Step regretReportStep) {
        return new JobBuilder("regret-report-job", jobRepository)
            .start(regretReportStep)
            .build();
    }

    @Bean
    public Step regretReportStep(RegretReportItemReader reader,
                                 RegretReportItemProcessor processor,
                                 RegretReportItemWriter writer,
                                 RetryPolicy batchRetryPolicy,
                                 SkipPolicy batchSkipPolicy,
                                 LoggingSkipListener<Object, Object> batchSkipListener) {
        return new StepBuilder("regret-report-step", jobRepository)
            .<RegretReportInput, RegretReport>chunk(CHUNK_SIZE)
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
