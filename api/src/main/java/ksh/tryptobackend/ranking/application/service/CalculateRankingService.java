package ksh.tryptobackend.ranking.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.ranking.application.port.in.CalculateRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.CalculateRankingCommand;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.ranking.application.port.out.RankingCommandPort;
import ksh.tryptobackend.ranking.application.port.out.TradingQueryPort;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.ActiveRounds;
import ksh.tryptobackend.ranking.domain.vo.EligibleRounds;
import ksh.tryptobackend.ranking.domain.vo.RankingCandidates;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import ksh.tryptobackend.ranking.domain.vo.RoundTradeCounts;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummaries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculateRankingService implements CalculateRankingUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final TradingQueryPort tradingQueryPort;
    private final PortfolioQueryPort portfolioQueryPort;
    private final RankingCommandPort rankingCommandPort;
    private final Clock clock;

    @Override
    public void calculateRanking(CalculateRankingCommand command) {
        LocalDate snapshotDate = command.snapshotDate();

        ActiveRounds activeRounds = investmentRoundQueryPort.findActiveRounds();
        RoundTradeCounts roundTradeCounts =
                tradingQueryPort.countTradesByRoundIds(activeRounds.roundIds());
        EligibleRounds eligibleRounds =
                activeRounds.toEligibleRounds(roundTradeCounts, snapshotDate);
        if (eligibleRounds.isEmpty()) {
            return;
        }

        SnapshotSummaries todaySummaries = portfolioQueryPort.findLatestSummaries(snapshotDate);

        for (RankingPeriod period : RankingPeriod.values()) {
            SnapshotSummaries comparison =
                    portfolioQueryPort.findLatestSummaries(
                            snapshotDate.minusDays(period.getWindowDays()));
            RankingCandidates candidates = eligibleRounds.toCandidates(todaySummaries, comparison);
            List<Ranking> rankings =
                    candidates.toRankings(period, snapshotDate, LocalDateTime.now(clock));
            rankingCommandPort.replaceByPeriodAndDate(rankings, period, snapshotDate);
        }
    }
}
