package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record RankingSummary(int rank, Long userId, BigDecimal profitRate, int tradeCount) {

    private static final int VIEWABLE_RANK_THRESHOLD = 100;

    public void assertViewable() {
        if (rank > VIEWABLE_RANK_THRESHOLD) {
            throw new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED);
        }
    }
}
