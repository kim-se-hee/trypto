package ksh.tryptobackend.ranking.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "total_asset", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalAsset;

    @Column(name = "total_asset_krw", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalAssetKrw;

    @Column(name = "total_investment", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalInvestment;

    @Column(name = "total_investment_krw", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalInvestmentKrw;

    @Column(name = "total_profit", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalProfit;

    @Column(name = "total_profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalProfitRate;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    public static PortfolioSnapshotJpaEntity fromDomain(PortfolioSnapshot snapshot) {
        PortfolioSnapshotJpaEntity entity = new PortfolioSnapshotJpaEntity();
        entity.id = snapshot.getId();
        entity.userId = snapshot.getUserId();
        entity.roundId = snapshot.getRoundId();
        entity.exchangeId = snapshot.getExchangeId();
        entity.totalAsset = snapshot.getTotalAsset();
        entity.totalAssetKrw = snapshot.getTotalAssetKrw();
        entity.totalInvestment = snapshot.getTotalInvestment();
        entity.totalInvestmentKrw = snapshot.getTotalInvestmentKrw();
        entity.totalProfit = snapshot.getTotalProfit();
        entity.totalProfitRate = snapshot.getTotalProfitRate();
        entity.snapshotDate = snapshot.getSnapshotDate();
        return entity;
    }

    public PortfolioSnapshot toDomain() {
        return PortfolioSnapshot.builder()
            .id(id)
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalAsset(totalAsset)
            .totalAssetKrw(totalAssetKrw)
            .totalInvestment(totalInvestment)
            .totalInvestmentKrw(totalInvestmentKrw)
            .totalProfit(totalProfit)
            .totalProfitRate(totalProfitRate)
            .snapshotDate(snapshotDate)
            .build();
    }
}
