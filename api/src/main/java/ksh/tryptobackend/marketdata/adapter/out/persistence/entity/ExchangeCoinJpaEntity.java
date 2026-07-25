package ksh.tryptobackend.marketdata.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "exchange_coin",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_exchange_coin",
                        columnNames = {"exchange_id", "coin_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeCoinJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_coin_id")
    private Long id;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MarketStatus status;

    public ExchangeCoinJpaEntity(Long exchangeId, Long coinId, String displayName) {
        this.exchangeId = exchangeId;
        this.coinId = coinId;
        this.displayName = displayName;
        this.status = MarketStatus.TRADING;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void applyStatus(MarketStatus status) {
        this.status = status;
    }

    public ExchangeCoin toDomain() {
        return new ExchangeCoin(id, exchangeId, coinId, displayName, status);
    }
}
