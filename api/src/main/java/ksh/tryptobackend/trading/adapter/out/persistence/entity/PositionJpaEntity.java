package ksh.tryptobackend.trading.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.Holding;
import ksh.tryptobackend.trading.domain.vo.Money;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "position", uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "coin_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
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

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PositionJpaEntity(Long walletId, Long coinId) {
        this.walletId = walletId;
        this.coinId = coinId;
        this.avgBuyPrice = BigDecimal.ZERO;
        this.totalQuantity = BigDecimal.ZERO;
        this.totalBuyAmount = BigDecimal.ZERO;
        this.averagingDownCount = 0;
    }

    public Position toDomain() {
        return Position.builder()
                .id(id)
                .walletId(walletId)
                .coinId(coinId)
                .holding(new Holding(Price.of(avgBuyPrice), Quantity.of(totalQuantity), Money.of(totalBuyAmount)))
                .averagingDownCount(averagingDownCount)
                .build();
    }

    public void updateFrom(Position position) {
        Holding holding = position.getHolding();
        this.avgBuyPrice = holding.avgBuyPrice().value();
        this.totalQuantity = holding.totalQuantity().value();
        this.totalBuyAmount = holding.totalBuyAmount().value();
        this.averagingDownCount = position.getAveragingDownCount();
    }
}
