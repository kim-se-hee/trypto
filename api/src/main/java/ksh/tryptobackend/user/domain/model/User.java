package ksh.tryptobackend.user.domain.model;

import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.Nickname;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

    private static final boolean DEFAULT_PORTFOLIO_PUBLIC = true;

    private final Long userId;
    private final Long version;
    private final SocialIdentity socialIdentity;
    private Nickname nickname;
    private boolean portfolioPublic;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User registerWith(SocialIdentity socialIdentity, Nickname nickname, LocalDateTime now) {
        return User.builder()
                .userId(null)
                .version(null)
                .socialIdentity(socialIdentity)
                .nickname(nickname)
                .portfolioPublic(DEFAULT_PORTFOLIO_PUBLIC)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User create(
            SocialIdentity socialIdentity, String nickname, boolean portfolioPublic, LocalDateTime now) {
        return User.builder()
                .userId(null)
                .version(null)
                .socialIdentity(socialIdentity)
                .nickname(Nickname.of(nickname))
                .portfolioPublic(portfolioPublic)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User reconstitute(
            Long userId,
            Long version,
            SocialIdentity socialIdentity,
            String nickname,
            boolean portfolioPublic,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return User.builder()
                .userId(userId)
                .version(version)
                .socialIdentity(socialIdentity)
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
