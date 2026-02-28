package ksh.tryptobackend.ranking.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "user")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingUserJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "portfolio_public", nullable = false)
    private boolean portfolioPublic;
}
