package ksh.tryptobackend.portfolio.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.portfolio.domain.model.SnapshotDetail;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "snapshot_id")
    private List<SnapshotDetailJpaEntity> details = new ArrayList<>();

    public static PortfolioSnapshotJpaEntity fromDomain(PortfolioSnapshot snapshot) {
        PortfolioSnapshotJpaEntity entity = new PortfolioSnapshotJpaEntity();
        entity.id = snapshot.getId();
        entity.userId = snapshot.getUserId();
        entity.roundId = snapshot.getRoundId();
        entity.exchangeId = snapshot.getExchangeId();
        entity.totalAsset = snapshot.getTotalAsset();
        entity.totalAssetKrw = snapshot.getTotalAssetKrw();
        entity.snapshotDate = snapshot.getSnapshotDate();
        entity.details = snapshot.getDetails().stream()
                .map(SnapshotDetailJpaEntity::fromDomain)
                .toList();
        return entity;
    }

    public PortfolioSnapshot toDomain() {
        List<SnapshotDetail> domainDetails =
                details.stream().map(SnapshotDetailJpaEntity::toDomain).toList();
        return PortfolioSnapshot.builder()
                .id(id)
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalAsset(totalAsset)
                .totalAssetKrw(totalAssetKrw)
                .snapshotDate(snapshotDate)
                .details(domainDetails)
                .build();
    }
}
