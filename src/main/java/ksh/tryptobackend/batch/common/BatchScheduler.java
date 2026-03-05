package ksh.tryptobackend.batch.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JobOperator jobOperator;
    private final Job snapshotJob;
    private final Job regretReportJob;
    private final Job rankingJob;
    private final TaskExecutor batchTaskExecutor;

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Seoul")
    public void runDailyBatch() {
        LocalDate snapshotDate = LocalDate.now(KST);
        JobParameters params = new JobParametersBuilder()
            .addString("snapshotDate", snapshotDate.toString())
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();

        log.info("배치 시작: snapshotDate={}", snapshotDate);

        if (!runSnapshotJob(params, snapshotDate)) {
            return;
        }

        runParallelJobs(params, snapshotDate);
    }

    private boolean runSnapshotJob(JobParameters params, LocalDate snapshotDate) {
        try {
            jobOperator.start(snapshotJob, params);
            log.info("SnapshotJob 완료");
            return true;
        } catch (Exception e) {
            log.error("SnapshotJob 실패 — 후속 배치 중단: snapshotDate={}", snapshotDate, e);
            return false;
        }
    }

    private void runParallelJobs(JobParameters params, LocalDate snapshotDate) {
        CompletableFuture<String> regretFuture = CompletableFuture
            .runAsync(() -> runJob(regretReportJob, params), batchTaskExecutor)
            .handle((result, ex) -> handleJobResult(regretReportJob.getName(), ex));

        CompletableFuture<String> rankingFuture = CompletableFuture
            .runAsync(() -> runJob(rankingJob, params), batchTaskExecutor)
            .handle((result, ex) -> handleJobResult(rankingJob.getName(), ex));

        CompletableFuture.allOf(regretFuture, rankingFuture).join();

        List<String> failedJobs = collectFailures(regretFuture, rankingFuture);
        if (failedJobs.isEmpty()) {
            log.info("배치 완료: snapshotDate={}", snapshotDate);
        } else {
            log.error("배치 부분 실패: snapshotDate={}, failedJobs={}", snapshotDate, failedJobs);
        }
    }

    private String handleJobResult(String jobName, Throwable ex) {
        if (ex != null) {
            log.error("{} 실패", jobName, ex);
            return jobName;
        }
        log.info("{} 완료", jobName);
        return null;
    }

    @SafeVarargs
    private List<String> collectFailures(CompletableFuture<String>... futures) {
        List<String> failedJobs = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            String failedJobName = future.join();
            if (failedJobName != null) {
                failedJobs.add(failedJobName);
            }
        }
        return failedJobs;
    }

    private void runJob(Job job, JobParameters params) {
        try {
            jobOperator.start(job, params);
        } catch (Exception e) {
            throw new RuntimeException(job.getName() + " 실패", e);
        }
    }
}
