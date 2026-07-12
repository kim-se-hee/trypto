package ksh.tryptobackend.user.domain.model;

import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialAccount {

    private final Long id;
    private final SocialIdentity socialIdentity;
    private Long userId;
    private final LocalDateTime createdAt;

    public static SocialAccount register(SocialIdentity socialIdentity, LocalDateTime now) {
        return SocialAccount.builder()
                .id(null)
                .socialIdentity(socialIdentity)
                .userId(null)
                .createdAt(now)
                .build();
    }

    public static SocialAccount reconstitute(
            Long id, SocialIdentity socialIdentity, Long userId, LocalDateTime createdAt) {
        return SocialAccount.builder()
                .id(id)
                .socialIdentity(socialIdentity)
                .userId(userId)
                .createdAt(createdAt)
                .build();
    }

    public void connectTo(Long userId) {
        this.userId = userId;
    }

    public boolean isConnected() {
        return userId != null;
    }
}
