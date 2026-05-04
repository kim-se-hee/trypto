package ksh.tryptobackend.marketdata.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "exchange_coin_chain")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeCoinChainJpaEntity {

    @Id
    @Column(name = "exchange_coin_chain_id")
    private Long id;

    @Column(name = "exchange_coin_id", nullable = false)
    private Long exchangeCoinId;

    @Column(name = "chain", nullable = false, length = 50)
    private String chain;

    @Column(name = "tag_required", nullable = false)
    private boolean tagRequired;

    public ExchangeCoinChain toDomain() {
        return ExchangeCoinChain.builder()
                .exchangeCoinChainId(id)
                .exchangeCoinId(exchangeCoinId)
                .chain(chain)
                .tagRequired(tagRequired)
                .build();
    }
}
