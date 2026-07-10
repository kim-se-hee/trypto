package ksh.tryptobackend.ranking.application.port.in.dto.result;

import ksh.tryptobackend.common.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.domain.vo.UserProfiles;

public record MyRankingResult(int rank, String nickname, ProfitRate profitRate, int tradeCount) {

    public static MyRankingResult of(RankingSummary summary, UserProfiles userProfiles) {
        return new MyRankingResult(
                summary.rank(),
                userProfiles.nicknameOf(summary.userId()),
                ProfitRate.of(summary.profitRate()),
                summary.tradeCount());
    }
}
