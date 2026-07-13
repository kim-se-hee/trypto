package ksh.tryptobackend.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "social_identity",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_social_identity_provider",
                    columnNames = {"provider", "provider_id"}),
            @UniqueConstraint(
                    name = "uk_social_identity_user",
                    columnNames = {"user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_identity_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SocialAccountJpaEntity fromDomain(SocialAccount socialAccount) {
        SocialAccountJpaEntity entity = new SocialAccountJpaEntity();
        entity.id = socialAccount.getId();
        SocialIdentity socialIdentity = socialAccount.getSocialIdentity();
        entity.provider = socialIdentity.providerName();
        entity.providerId = socialIdentity.providerId();
        entity.userId = socialAccount.getUserId();
        entity.createdAt = socialAccount.getCreatedAt();
        return entity;
    }

    public void connectTo(Long userId) {
        this.userId = userId;
    }

    public void updateFromDomain(SocialAccount socialAccount) {
        this.userId = socialAccount.getUserId();
    }

    public SocialAccount toDomain() {
        return SocialAccount.reconstitute(
                id, SocialIdentity.of(Provider.valueOf(provider), providerId), userId, createdAt);
    }
}
