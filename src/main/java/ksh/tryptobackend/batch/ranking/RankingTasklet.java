package ksh.tryptobackend.batch.ranking;

import ksh.tryptobackend.ranking.application.port.in.CalculateRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.CalculateRankingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@StepScope
@RequiredArgsConstructor
public class RankingTasklet implements Tasklet {

    private final CalculateRankingUseCase calculateRankingUseCase;
    private final RetryPolicy batchRetryPolicy;

    @Value("#{jobParameters['snapshotDate']}")
    private String snapshotDateParam;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate snapshotDate = LocalDate.parse(snapshotDateParam);
        RetryTemplate retryTemplate = new RetryTemplate(batchRetryPolicy);
        retryTemplate.invoke(() ->
            calculateRankingUseCase.calculateRanking(new CalculateRankingCommand(snapshotDate))
        );
        return RepeatStatus.FINISHED;
    }
}
