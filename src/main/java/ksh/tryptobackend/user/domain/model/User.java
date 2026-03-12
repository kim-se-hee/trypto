package ksh.tryptobackend.user.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;

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
        validateSameNickname(newNickname);
        validateNicknameLength(newNickname);
        this.nickname = newNickname;
    }

    private void validateSameNickname(String newNickname) {
        if (this.nickname.equals(newNickname)) {
            throw new CustomException(ErrorCode.NICKNAME_SAME_AS_CURRENT);
        }
    }

    private void validateNicknameLength(String newNickname) {
        if (newNickname.length() < MIN_NICKNAME_LENGTH || newNickname.length() > MAX_NICKNAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME_LENGTH);
        }
    }

    public void changePortfolioVisibility(boolean portfolioPublic) {
        this.portfolioPublic = portfolioPublic;
    }
}
