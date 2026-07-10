package ksh.tryptobackend.user.adapter.in.dto.response;

import ksh.tryptobackend.user.domain.model.User;

public record ChangePortfolioVisibilityResponse(Long userId, boolean portfolioPublic) {

    public static ChangePortfolioVisibilityResponse from(User user) {
        return new ChangePortfolioVisibilityResponse(user.getUserId(), user.isPortfolioPublic());
    }
}
