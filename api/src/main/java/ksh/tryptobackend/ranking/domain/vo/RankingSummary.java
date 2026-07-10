package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.domain.model.Ranking;

public record RankingSummary(int rank, Long userId, BigDecimal profitRate, int tradeCount) {

    public void assertViewable() {
        if (!Ranking.isTop100(rank)) {
            throw new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED);
        }
    }
}
