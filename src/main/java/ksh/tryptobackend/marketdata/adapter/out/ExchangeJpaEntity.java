package ksh.tryptobackend.marketdata.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchange_market")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeJpaEntity {

    @Id
    @Column(name = "exchange_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    private ExchangeMarketType marketType;

    @Column(name = "base_currency_coin_id", nullable = false)
    private Long baseCurrencyCoinId;

    public ExchangeJpaEntity(Long id, String name, ExchangeMarketType marketType, Long baseCurrencyCoinId) {
        this.id = id;
        this.name = name;
        this.marketType = marketType;
        this.baseCurrencyCoinId = baseCurrencyCoinId;
    }

    public Exchange toDomain() {
        return Exchange.builder()
            .exchangeId(id)
            .name(name)
            .marketType(marketType)
            .baseCurrencyCoinId(baseCurrencyCoinId)
            .build();
    }
}
