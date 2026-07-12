package ksh.tryptobackend.user.adapter.in.dto.response;

import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.model.User;

public record UserProfileResponse(Long userId, String nickname, boolean portfolioPublic, LocalDateTime createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getUserId(), user.getNickname().value(), user.isPortfolioPublic(), user.getCreatedAt());
    }
}
