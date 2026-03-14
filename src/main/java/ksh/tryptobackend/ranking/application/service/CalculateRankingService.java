package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotSummariesUseCase;
import ksh.tryptobackend.ranking.application.port.in.CalculateRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.CalculateRankingCommand;
import ksh.tryptobackend.ranking.application.port.out.RankingCommandPort;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.EligibleRound;
import ksh.tryptobackend.ranking.domain.vo.EligibleRounds;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidates;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import ksh.tryptobackend.ranking.domain.vo.RoundTradeCounts;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummaries;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummary;
import ksh.tryptobackend.trading.application.port.in.CountTradesByRoundIdsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalculateRankingService implements CalculateRankingUseCase {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;
    private final CountTradesByRoundIdsUseCase countTradesByRoundIdsUseCase;
    private final FindSnapshotSummariesUseCase findSnapshotSummariesUseCase;
    private final RankingCommandPort rankingCommandPort;
    private final Clock clock;

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
            List<Ranking> rankings = candidates.toRankings(period, snapshotDate, LocalDateTime.now(clock));
            rankingCommandPort.replaceByPeriodAndDate(rankings, period, snapshotDate);
        }
    }

    private EligibleRounds findEligibleRounds(LocalDate snapshotDate) {
        List<ActiveRoundResult> activeRounds = findActiveRoundsUseCase.findAllActiveRounds();
        List<Long> roundIds = activeRounds.stream().map(ActiveRoundResult::roundId).toList();

        RoundTradeCounts roundTradeCounts = toRoundTradeCounts(roundIds);

        List<EligibleRound> eligibleRoundList = activeRounds.stream()
            .map(round -> toEligibleRound(round, roundTradeCounts))
            .toList();

        return EligibleRounds.of(eligibleRoundList, snapshotDate);
    }

    private RoundTradeCounts toRoundTradeCounts(List<Long> roundIds) {
        Map<Long, Integer> countByRoundId = countTradesByRoundIdsUseCase.countTradesByRoundIds(roundIds);
        return new RoundTradeCounts(countByRoundId);
    }

    private EligibleRound toEligibleRound(ActiveRoundResult round, RoundTradeCounts roundTradeCounts) {
        return new EligibleRound(
            round.userId(), round.roundId(),
            roundTradeCounts.getCount(round.roundId()),
            round.startedAt()
        );
    }

    private SnapshotSummaries loadSummariesOf(LocalDate date) {
        List<SnapshotSummary> summaries = findSnapshotSummariesUseCase.findLatestSummaries(date).stream()
            .map(r -> new SnapshotSummary(r.userId(), r.roundId(), r.totalAssetKrw(), r.totalInvestmentKrw()))
            .toList();
        return new SnapshotSummaries(summaries);
    }
}
