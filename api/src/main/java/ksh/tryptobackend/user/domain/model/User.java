package ksh.tryptobackend.user.domain.model;

import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.Nickname;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

    private final Long userId;
    private final String email;
    private Nickname nickname;
    private boolean portfolioPublic;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User reconstitute(
            Long userId,
            String email,
            String nickname,
            boolean portfolioPublic,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return User.builder()
                .userId(userId)
                .email(email)
                .nickname(Nickname.of(nickname))
                .portfolioPublic(portfolioPublic)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public void changeNickname(String newNickname) {
        if (nickname.hasSameValueAs(newNickname)) {
            throw new CustomException(ErrorCode.NICKNAME_SAME_AS_CURRENT);
        }
        this.nickname = Nickname.of(newNickname);
    }

    public void changePortfolioVisibility(boolean portfolioPublic) {
        this.portfolioPublic = portfolioPublic;
    }
}
