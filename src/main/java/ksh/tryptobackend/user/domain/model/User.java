package ksh.tryptobackend.user.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private final Long userId;
    private final String email;
    private String nickname;
    private boolean portfolioPublic;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User reconstitute(Long userId, String email, String nickname,
                                     boolean portfolioPublic,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return User.builder()
            .userId(userId)
            .email(email)
            .nickname(nickname)
            .portfolioPublic(portfolioPublic)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changePortfolioVisibility(boolean portfolioPublic) {
        this.portfolioPublic = portfolioPublic;
    }
}
