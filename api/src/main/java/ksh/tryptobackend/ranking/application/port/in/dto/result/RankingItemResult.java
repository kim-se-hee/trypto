package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.domain.vo.UserProfiles;

public record RankingItemResult(
        int rank,
        Long userId,
        String nickname,
        BigDecimal profitRate,
        int tradeCount,
        boolean portfolioPublic) {

    public static RankingItemResult of(RankingSummary summary, UserProfiles userProfiles) {
        return new RankingItemResult(
                summary.rank(),
                summary.userId(),
                userProfiles.nicknameOf(summary.userId()),
                summary.profitRate(),
                summary.tradeCount(),
                userProfiles.isPortfolioPublicOf(summary.userId()));
    }
}
