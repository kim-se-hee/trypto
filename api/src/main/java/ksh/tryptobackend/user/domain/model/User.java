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

    private static final int RESIGNUP_RESTRICTION_DAYS = 30;

    private final Long userId;
    private final Long version;
    private final Long socialAccountId;
    private final LocalDateTime createdAt;
    private Nickname nickname;
    private LocalDateTime deletedAt;
    private LocalDateTime updatedAt;

    public static User registerWith(Long socialAccountId, Nickname nickname, LocalDateTime now) {
        return User.builder()
                .userId(null)
                .version(null)
                .socialAccountId(socialAccountId)
                .nickname(nickname)
                .deletedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User create(Long socialAccountId, String nickname, LocalDateTime now) {
        return User.builder()
                .userId(null)
                .version(null)
                .socialAccountId(socialAccountId)
                .nickname(Nickname.of(nickname))
                .deletedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User reconstitute(
            Long userId,
            Long version,
            Long socialAccountId,
            String nickname,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return User.builder()
                .userId(userId)
                .version(version)
                .socialAccountId(socialAccountId)
                .nickname(Nickname.of(nickname))
                .deletedAt(deletedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static void ensureReSignupAllowed(LocalDateTime lastWithdrawnAt, LocalDateTime now) {
        if (!now.isAfter(lastWithdrawnAt.plusDays(RESIGNUP_RESTRICTION_DAYS))) {
            throw new CustomException(ErrorCode.SIGNUP_RESTRICTED);
        }
    }

    public void withdraw(Nickname anonymousNickname, LocalDateTime now) {
        if (isWithdrawn()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }
        this.nickname = anonymousNickname;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void changeNickname(String newNickname) {
        if (nickname.hasSameValueAs(newNickname)) {
            throw new CustomException(ErrorCode.NICKNAME_SAME_AS_CURRENT);
        }
        this.nickname = Nickname.of(newNickname);
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }
}
