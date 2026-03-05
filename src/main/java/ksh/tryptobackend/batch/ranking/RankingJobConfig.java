package ksh.tryptobackend.batch.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class RankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job rankingJob(Step rankingStep) {
        return new JobBuilder("ranking-job", jobRepository)
            .start(rankingStep)
            .build();
    }

    @Bean
    public Step rankingStep(RankingTasklet tasklet) {
        return new StepBuilder("ranking-step", jobRepository)
            .tasklet(tasklet)
            .transactionManager(transactionManager)
            .build();
    }
}
