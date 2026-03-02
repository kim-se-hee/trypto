package ksh.tryptobackend.marketdata.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "coin")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinJpaEntity {

    @Id
    @Column(name = "coin_id")
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol;
}
