package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.ranking.domain.vo.HoldingView;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.domain.vo.UserProfile;

public record RankerPortfolioResult(
        Long userId, String nickname, int rank, BigDecimal profitRate, List<PortfolioHoldingResult> holdings) {

    public static RankerPortfolioResult of(RankingSummary ranking, UserProfile viewer, List<HoldingView> holdings) {
        return new RankerPortfolioResult(
                ranking.userId(),
                viewer.nickname(),
                ranking.rank(),
                ranking.profitRate(),
                holdings.stream().map(PortfolioHoldingResult::from).toList());
    }
}
