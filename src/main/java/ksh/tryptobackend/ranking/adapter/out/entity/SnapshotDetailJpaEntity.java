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

@Entity
@Table(name = "portfolio_snapshot_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnapshotDetailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "quantity", nullable = false, precision = 30, scale = 8)
    private BigDecimal quantity;

    @Column(name = "avg_buy_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal avgBuyPrice;

    @Column(name = "current_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "asset_ratio", nullable = false, precision = 10, scale = 4)
    private BigDecimal assetRatio;
}
