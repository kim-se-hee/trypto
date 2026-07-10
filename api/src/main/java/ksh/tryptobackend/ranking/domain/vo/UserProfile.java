package ksh.tryptobackend.ranking.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record UserProfile(Long userId, String nickname, boolean portfolioPublic) {

    public void assertPortfolioPublic() {
        if (!portfolioPublic) {
            throw new CustomException(ErrorCode.PORTFOLIO_PRIVATE);
        }
    }
}
