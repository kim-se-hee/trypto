package ksh.tryptobackend.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.model.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "\"user\"",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_nickname",
                    columnNames = {"nickname"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "social_identity_id", nullable = false)
    private Long socialAccountId;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "portfolio_public", nullable = false)
    private boolean portfolioPublic;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getUserId();
        entity.version = user.getVersion();
        entity.socialAccountId = user.getSocialAccountId();
        entity.nickname = user.getNickname().value();
        entity.portfolioPublic = user.isPortfolioPublic();
        entity.deletedAt = user.getDeletedAt();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public void updateFromDomain(User user) {
        this.nickname = user.getNickname().value();
        this.portfolioPublic = user.isPortfolioPublic();
        this.deletedAt = user.getDeletedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public User toDomain() {
        return User.reconstitute(
                id, version, socialAccountId, nickname, portfolioPublic, deletedAt, createdAt, updatedAt);
    }
}
