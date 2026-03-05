package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.CalculateRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.CalculateRankingCommand;
import ksh.tryptobackend.ranking.application.port.out.EligibleRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.RankingWritePort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotAggregationPort;
import ksh.tryptobackend.ranking.application.port.out.dto.UserSnapshotSummary;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.EligibleRounds;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidates;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import ksh.tryptobackend.ranking.domain.vo.RoundKey;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummaries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculateRankingService implements CalculateRankingUseCase {

    private final EligibleRoundQueryPort eligibleRoundQueryPort;
    private final SnapshotAggregationPort snapshotAggregationPort;
    private final RankingWritePort rankingWritePort;

    @Override
    public void calculateRanking(CalculateRankingCommand command) {
        LocalDate snapshotDate = command.snapshotDate();

        EligibleRounds eligibleRounds = findEligibleRounds(snapshotDate);
        if (eligibleRounds.isEmpty()) {
            return;
        }

        SnapshotSummaries todaySummaries = loadSummariesOf(snapshotDate);

        for (RankingPeriod period : RankingPeriod.values()) {
            SnapshotSummaries comparison = loadSummariesOf(snapshotDate.minusDays(period.getWindowDays()));
            RankingCandidates candidates = eligibleRounds.toCandidates(todaySummaries, comparison);
            List<Ranking> rankings = candidates.toRankings(period, snapshotDate);
            rankingWritePort.replaceByPeriodAndDate(rankings, period, snapshotDate);
        }
    }

    private EligibleRounds findEligibleRounds(LocalDate snapshotDate) {
        return EligibleRounds.of(eligibleRoundQueryPort.findAll(), snapshotDate);
    }

    private SnapshotSummaries loadSummariesOf(LocalDate date) {
        List<UserSnapshotSummary> summaries = snapshotAggregationPort.findLatestSummaries(date);

        Map<RoundKey, BigDecimal> totalAssetMap = summaries.stream()
            .collect(Collectors.toMap(UserSnapshotSummary::roundKey, UserSnapshotSummary::totalAssetKrw));

        return new SnapshotSummaries(totalAssetMap);
    }

}
