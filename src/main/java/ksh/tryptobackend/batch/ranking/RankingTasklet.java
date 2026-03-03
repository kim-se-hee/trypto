package ksh.tryptobackend.batch.ranking;

import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.RankingEligibilityPort;
import ksh.tryptobackend.ranking.application.port.out.RankingWritePort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotAggregationPort;
import ksh.tryptobackend.ranking.application.port.out.TradeCountPort;
import ksh.tryptobackend.ranking.application.port.out.WalletSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ActiveRoundInfo;
import ksh.tryptobackend.ranking.application.port.out.dto.UserSnapshotSummary;
import ksh.tryptobackend.ranking.application.port.out.dto.WalletSnapshotInfo;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidate;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class RankingTasklet implements Tasklet {

    private static final int RATE_SCALE = 4;
    private static final int ELIGIBILITY_HOURS = 24;

    private final ActiveRoundQueryPort activeRoundQueryPort;
    private final WalletSnapshotPort walletSnapshotPort;
    private final RankingEligibilityPort rankingEligibilityPort;
    private final TradeCountPort tradeCountPort;
    private final SnapshotAggregationPort snapshotAggregationPort;
    private final RankingWritePort rankingWritePort;

    @Value("#{jobParameters['snapshotDate']}")
    private String snapshotDateParam;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate snapshotDate = LocalDate.parse(snapshotDateParam);
        LocalDateTime eligibilityCutoff = snapshotDate.atStartOfDay().minusHours(ELIGIBILITY_HOURS);

        List<ActiveRoundInfo> eligibleRounds = filterEligibleRounds(eligibilityCutoff);
        if (eligibleRounds.isEmpty()) {
            return RepeatStatus.FINISHED;
        }

        Map<RoundKey, UserSnapshotSummary> todaySummaryMap = buildSummaryMap(snapshotDate);
        Map<Long, Integer> tradeCountMap = buildTradeCountMap(eligibleRounds);

        for (RankingPeriod period : RankingPeriod.values()) {
            processRankingForPeriod(period, snapshotDate, eligibleRounds, todaySummaryMap, tradeCountMap);
        }

        return RepeatStatus.FINISHED;
    }

    private List<ActiveRoundInfo> filterEligibleRounds(LocalDateTime eligibilityCutoff) {
        return activeRoundQueryPort.findAllActiveRounds().stream()
            .filter(round -> round.startedAt().isBefore(eligibilityCutoff))
            .filter(this::hasAnyFilledOrder)
            .toList();
    }

    private boolean hasAnyFilledOrder(ActiveRoundInfo round) {
        return walletSnapshotPort.findByRoundId(round.roundId()).stream()
            .anyMatch(wallet -> rankingEligibilityPort.hasFilledOrders(wallet.walletId()));
    }

    private Map<RoundKey, UserSnapshotSummary> buildSummaryMap(LocalDate date) {
        return snapshotAggregationPort.findLatestSummaries(date).stream()
            .collect(Collectors.toMap(
                s -> new RoundKey(s.userId(), s.roundId()),
                s -> s
            ));
    }

    private Map<Long, Integer> buildTradeCountMap(List<ActiveRoundInfo> eligibleRounds) {
        return eligibleRounds.stream()
            .collect(Collectors.toMap(
                ActiveRoundInfo::roundId,
                round -> countTradesForRound(round.roundId())
            ));
    }

    private int countTradesForRound(Long roundId) {
        return walletSnapshotPort.findByRoundId(roundId).stream()
            .mapToInt(wallet -> tradeCountPort.countFilledOrders(wallet.walletId()))
            .sum();
    }

    private void processRankingForPeriod(RankingPeriod period, LocalDate snapshotDate,
                                         List<ActiveRoundInfo> eligibleRounds,
                                         Map<RoundKey, UserSnapshotSummary> todaySummaryMap,
                                         Map<Long, Integer> tradeCountMap) {
        LocalDate comparisonDate = snapshotDate.minusDays(period.getWindowDays());
        Map<RoundKey, UserSnapshotSummary> comparisonSummaryMap = buildSummaryMap(comparisonDate);

        List<RankingCandidate> candidates = buildCandidates(
            eligibleRounds, todaySummaryMap, comparisonSummaryMap, tradeCountMap
        );
        Collections.sort(candidates);

        List<Ranking> rankings = assignRanks(candidates, period, snapshotDate);
        saveForPeriod(rankings, period, snapshotDate);
    }

    private List<RankingCandidate> buildCandidates(List<ActiveRoundInfo> eligibleRounds,
                                                   Map<RoundKey, UserSnapshotSummary> todaySummaryMap,
                                                   Map<RoundKey, UserSnapshotSummary> comparisonSummaryMap,
                                                   Map<Long, Integer> tradeCountMap) {
        List<RankingCandidate> candidates = new ArrayList<>();
        for (ActiveRoundInfo round : eligibleRounds) {
            RoundKey key = new RoundKey(round.userId(), round.roundId());
            UserSnapshotSummary todaySummary = todaySummaryMap.get(key);
            UserSnapshotSummary comparisonSummary = comparisonSummaryMap.get(key);

            if (todaySummary == null || comparisonSummary == null) {
                continue;
            }

            BigDecimal profitRate = calculateWindowProfitRate(
                todaySummary.totalAssetKrw(), comparisonSummary.totalAssetKrw()
            );
            int tradeCount = tradeCountMap.getOrDefault(round.roundId(), 0);

            candidates.add(new RankingCandidate(
                round.userId(), round.roundId(), profitRate, tradeCount, round.startedAt()
            ));
        }
        return candidates;
    }

    private BigDecimal calculateWindowProfitRate(BigDecimal todayAsset, BigDecimal comparisonAsset) {
        if (comparisonAsset.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return todayAsset.subtract(comparisonAsset)
            .divide(comparisonAsset, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    private List<Ranking> assignRanks(List<RankingCandidate> candidates, RankingPeriod period,
                                      LocalDate referenceDate) {
        List<Ranking> rankings = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            RankingCandidate candidate = candidates.get(i);
            rankings.add(Ranking.create(
                candidate.userId(), candidate.roundId(), period,
                i + 1, ProfitRate.of(candidate.profitRate()), candidate.tradeCount(),
                referenceDate
            ));
        }
        return rankings;
    }

    private void saveForPeriod(List<Ranking> rankings, RankingPeriod period, LocalDate referenceDate) {
        rankingWritePort.deleteByPeriodAndDate(period, referenceDate);
        rankingWritePort.saveAll(rankings);
    }

    private record RoundKey(Long userId, Long roundId) {}
}
