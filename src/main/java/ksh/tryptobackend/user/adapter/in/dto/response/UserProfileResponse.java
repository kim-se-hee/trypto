package ksh.tryptobackend.user.adapter.in.dto.response;

import ksh.tryptobackend.user.domain.model.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
    Long userId,
    String email,
    String nickname,
    boolean portfolioPublic,
    LocalDateTime createdAt
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getUserId(),
            user.getEmail(),
            user.getNickname(),
            user.isPortfolioPublic(),
            user.getCreatedAt()
        );
    }
}
