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
@Table(name = "exchange_market")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingExchangeJpaEntity {

    @Id
    @Column(name = "exchange_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
}
