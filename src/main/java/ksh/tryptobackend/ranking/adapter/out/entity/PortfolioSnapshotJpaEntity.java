package ksh.tryptobackend.ranking.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "total_asset_krw", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalAssetKrw;

    @Column(name = "total_profit_krw", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalProfitKrw;

    @Column(name = "total_profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalProfitRate;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;
}
