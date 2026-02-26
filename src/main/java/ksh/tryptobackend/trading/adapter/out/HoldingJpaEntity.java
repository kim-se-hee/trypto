package ksh.tryptobackend.trading.adapter.out;

import jakarta.persistence.*;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "avg_buy_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal avgBuyPrice;

    @Column(name = "total_quantity", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalQuantity;

    @Column(name = "total_buy_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalBuyAmount;

    @Column(name = "averaging_down_count", nullable = false)
    private int averagingDownCount;

    public HoldingJpaEntity(Long walletId, Long coinId) {
        this.walletId = walletId;
        this.coinId = coinId;
        this.avgBuyPrice = BigDecimal.ZERO;
        this.totalQuantity = BigDecimal.ZERO;
        this.totalBuyAmount = BigDecimal.ZERO;
        this.averagingDownCount = 0;
    }

    public Holding toDomain() {
        return Holding.builder()
            .id(id)
            .walletId(walletId)
            .coinId(coinId)
            .avgBuyPrice(avgBuyPrice)
            .totalQuantity(totalQuantity)
            .totalBuyAmount(totalBuyAmount)
            .averagingDownCount(averagingDownCount)
            .build();
    }

    public void updateFrom(Holding holding) {
        this.avgBuyPrice = holding.getAvgBuyPrice();
        this.totalQuantity = holding.getTotalQuantity();
        this.totalBuyAmount = holding.getTotalBuyAmount();
        this.averagingDownCount = holding.getAveragingDownCount();
    }
}
