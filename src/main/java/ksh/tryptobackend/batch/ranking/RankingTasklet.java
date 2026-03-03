package ksh.tryptobackend.batch.ranking;

import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.RankingWritePort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotAggregationPort;
import ksh.tryptobackend.ranking.application.port.out.TradeCountPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ActiveRoundInfo;
import ksh.tryptobackend.ranking.application.port.out.dto.RoundKey;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidate;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidates;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class RankingTasklet implements Tasklet {

    private static final int ELIGIBILITY_HOURS = 24;

    private final ActiveRoundQueryPort activeRoundQueryPort;
    private final TradeCountPort tradeCountPort;
    private final SnapshotAggregationPort snapshotAggregationPort;
    private final RankingWritePort rankingWritePort;

    @Value("#{jobParameters['snapshotDate']}")
    private String snapshotDateParam;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate snapshotDate = LocalDate.parse(snapshotDateParam);

        List<EligibleRound> eligibleRounds = filterEligibleRounds(snapshotDate);
        if (eligibleRounds.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        SnapshotSummaries todaySummaries = loadSummaries(snapshotDate);

        for (RankingPeriod period : RankingPeriod.values()) {
            SnapshotSummaries comparisonSummaries =
                loadSummaries(snapshotDate.minusDays(period.getWindowDays()));
            RankingCandidates candidates = buildCandidates(eligibleRounds, todaySummaries, comparisonSummaries);
            List<Ranking> rankings = candidates.toRankings(period, snapshotDate);
            saveForPeriod(rankings, period, snapshotDate);
        }

        return RepeatStatus.FINISHED;
    }

    private List<EligibleRound> filterEligibleRounds(LocalDate snapshotDate) {
        LocalDateTime eligibilityCutoff = snapshotDate.atStartOfDay().minusHours(ELIGIBILITY_HOURS);

        List<ActiveRoundInfo> timeEligibleRounds = activeRoundQueryPort.findAllActiveRounds().stream()
            .filter(round -> round.isStartedBefore(eligibilityCutoff))
            .toList();

        List<Long> roundIds = timeEligibleRounds.stream().map(ActiveRoundInfo::roundId).toList();
        Map<Long, Integer> tradeCountMap = tradeCountPort.countFilledOrdersByRoundIds(roundIds);

        return timeEligibleRounds.stream()
            .filter(round -> tradeCountMap.getOrDefault(round.roundId(), 0) > 0)
            .map(round -> new EligibleRound(
                round.userId(), round.roundId(),
                tradeCountMap.get(round.roundId()), round.startedAt()
            ))
            .toList();
    }

    private SnapshotSummaries loadSummaries(LocalDate date) {
        return new SnapshotSummaries(snapshotAggregationPort.findLatestSummaries(date));
    }

    private RankingCandidates buildCandidates(List<EligibleRound> eligibleRounds,
                                               SnapshotSummaries todaySummaries,
                                               SnapshotSummaries comparisonSummaries) {
        List<RankingCandidate> candidates = new ArrayList<>();
        for (EligibleRound round : eligibleRounds) {
            todaySummaries.calculateProfitRate(round.roundKey(), comparisonSummaries)
                .ifPresent(profitRate -> candidates.add(new RankingCandidate(
                    round.userId(), round.roundId(), profitRate, round.tradeCount(), round.startedAt()
                )));
        }
        return new RankingCandidates(candidates);
    }

    private void saveForPeriod(List<Ranking> rankings, RankingPeriod period, LocalDate referenceDate) {
        rankingWritePort.deleteByPeriodAndDate(period, referenceDate);
        rankingWritePort.saveAll(rankings);
    }

    private record EligibleRound(Long userId, Long roundId, int tradeCount, LocalDateTime startedAt) {
        RoundKey roundKey() {
            return new RoundKey(userId, roundId);
        }
    }
}
