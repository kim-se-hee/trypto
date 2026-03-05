package ksh.tryptobackend.ranking.domain.model;

import ksh.tryptobackend.ranking.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class Ranking {

    private static final int TOP_RANK_THRESHOLD = 100;

    private final Long id;
    private final Long userId;
    private final Long roundId;
    private final RankingPeriod period;
    private final int rank;
    private final ProfitRate profitRate;
    private final int tradeCount;
    private final LocalDate referenceDate;
    private final LocalDateTime createdAt;

    public static Ranking create(Long userId, Long roundId, RankingPeriod period,
                                  int rank, ProfitRate profitRate, int tradeCount,
                                  LocalDate referenceDate) {
        return Ranking.builder()
            .userId(userId)
            .roundId(roundId)
            .period(period)
            .rank(rank)
            .profitRate(profitRate)
            .tradeCount(tradeCount)
            .referenceDate(referenceDate)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static boolean isTop100(int rank) {
        return rank <= TOP_RANK_THRESHOLD;
    }
}
